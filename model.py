import csv
import cv2
import numpy as np
import sklearn
from keras.models import Sequential
from keras.layers import Flatten, Dense, Lambda, Convolution2D, Cropping2D
from keras import regularizers
from sklearn.model_selection import train_test_split

lines = []
with open('./data/driving_log.csv') as csvfile:
    reader = csv.reader(csvfile)
    for line in reader:
        lines.append(line)
batch_size = 512


def generator(samples, batch_size=64):
    num_samples = len(samples)
    correction = .2
    while 1: # Loop forever so the generator never terminates
        sklearn.utils.shuffle(samples)
        for offset in range(0, num_samples, batch_size):
            batch_samples = samples[offset:offset+batch_size]

            images = []
            angles = []
            for batch_sample in batch_samples:
                #Load images using opencv
                name = './data/IMG/'+batch_sample[0].split('/')[-1]
                left_name = './data/IMG/'+batch_sample[1].split('/')[-1]
                right_name = './data/IMG/'+batch_sample[2].split('/')[-1]
                center_image = cv2.imread(name)
                center_image = cv2.cvtColor(center_image, cv2.COLOR_BGR2RGB)
                img_left = cv2.imread(left_name)
                #Convert to RGB 
                img_left = cv2.cvtColor(img_left, cv2.COLOR_BGR2RGB)
                img_right = cv2.imread(right_name)
                img_right = cv2.cvtColor(img_right, cv2.COLOR_BGR2RGB)
                center_angle = float(batch_sample[3])
                images.append(center_image)
                angles.append(center_angle)
                #Flip center image
                images.append(cv2.flip(center_image,1))
                #Flip center angle
                angles.append(center_angle*-1.0)
                #Add additional camera images
                images.append(img_left)
                images.append(img_right)
                angles.append(center_angle+.3)
                angles.append(center_angle-correction)

            X_train = np.array(images)
            y_train = np.array(angles)
            yield sklearn.utils.shuffle(X_train, y_train)


train_samples, validation_samples = train_test_split(lines, test_size=0.2)
#Split data into training and validation sets
train_generator = generator(train_samples, batch_size=32)
validation_generator = generator(validation_samples, batch_size=32)


# Initialize the model
model = Sequential()
# Normalize the data
model.add(Lambda(lambda x: x / 255.0 - 0.5, input_shape=(160,320,3)))
# Crop the image to the area of interest
model.add(Cropping2D(cropping=((70,25),(0,0))))
# Nvidia model
model.add(Convolution2D(24,5,5,subsample=(2,2),activation="relu"))
model.add(Convolution2D(36,5,5,subsample=(2,2),activation="relu"))
model.add(Convolution2D(48,5,5,subsample=(2,2),activation="relu"))
model.add(Convolution2D(64,3,3,activation="relu"))
model.add(Convolution2D(64,3,3,activation="relu"))
model.add(Flatten())
model.add(Dense(100))
model.add(Dense(50))
model.add(Dense(10))
# Add L2 regularization
model.add(Dense(1,kernel_regularizer=regularizers.l2(0.01)))
model.compile(loss='mse', optimizer='adam')

model.fit_generator(train_generator, validation_data=validation_generator,
                                 steps_per_epoch = len(train_samples)/batch_size,
                                 nb_val_samples = len(validation_samples)/batch_size,                                  epochs=3)
model.save('model.h5')
