import numpy as np
import os

import tensorflow as tf
#print(tf.reduce_sum(tf.random.normal([1000, 1000])))
#print(tf.config.list_physical_devices('GPU'))
from tflite_model_maker.config import QuantizationConfig
from tflite_model_maker import model_spec
from tflite_model_maker.config import ExportFormat
import tflite_model_maker
from tflite_model_maker import object_detector
from PIL import Image
import glob
import xml.etree.ElementTree as ET
import csv
import os

assert tf.__version__.startswith('2')
if tf.config.list_physical_devices('GPU'):
    physical_devices = tf.config.list_physical_devices('GPU')
    tf.config.experimental.set_memory_growth(physical_devices[0], enable=True)
    tf.config.experimental.set_virtual_device_configuration(physical_devices[0], [tf.config.experimental.VirtualDeviceConfiguration(memory_limit=4000)])
tf.get_logger().setLevel('ERROR')

from absl import logging
logging.set_verbosity(logging.ERROR)
#tf.debugging.set_log_device_placement(True)

def load(filepath,filename,separator=','):

    #open and read csv file
    file=open(filepath+filename,'r').read()
    lines=file.split('\n')

    #legend's format: width,height, 4 points bndbox [x,y,x+width,y+height],class Id photo, Path to photo
    legend=["Width","Height","Roi.X1",'Roi.Y1','Roi.X2','Roi.Y2','ClassId','Path']

    listDict=[]
    #save information about each photo to list
    for line in lines:
        item = line.split(separator)

        if len(item)>1:
            dict = {}
            for i in range(len(legend)):
                dict[legend[i]]=item[i]
        listDict.append(dict)
    return listDict

def add_photo_size(data,imagesPath):
    for i in range(len(data)):
        img=Image.open(imagesPath + data[i]['Path'])
        data[i].update({'Width':img.width,'Height':img.height})
    return data



def load_xml(filepath):
    fileformat='xml'
    xml_files=[]
    for each_file in glob.glob(filepath+'annotations\\*.{}'.format(fileformat)):
        xml_files.append(each_file)
    data=[]
    for each in xml_files:


        size_image = []
        list_objects_on_image =[]
        tree=ET.ElementTree(file=each)
        root=tree.getroot()
        file_name=root[1].text
        folder=root[0].text
        path=folder+'/'+file_name
        size_image.append({'width':root[2][0].text,'height':root[2][1].text,'depth':root[2][2].text})
        for child in root.iter('object'):
            bndbox = []
            for bndbox_ in child.iter('bndbox'):
                bndbox.append([bndbox_[0].text,bndbox_[1].text,bndbox_[2].text,bndbox_[3].text])
                data.append({'Path': path, 'Width': size_image[0]['width'], 'Height': size_image[0]['height'],
                             'Roi.X1':bndbox_[0].text,'Roi.Y1':bndbox_[1].text,'Roi.X2':bndbox_[2].text,'Roi.Y2':bndbox_[3].text,'ClassId':child[0].text})
            #list_objects_on_image.append({'object':child[0].text,'bndbox':bndbox[0]})

        #data.append({'Path':path,'Width':size_image[0]['width'],'Height':size_image[0]['height'],'objects':list_objects_on_image})
    return data

def load_txt_and_convert_from_ppm_to_png(filePath, fileName, saveFilePath):
    try:
        file = open(filePath + fileName, 'r').read()
    except:
        pass

    lines = file.split('\n')
    data=[]
    for each in lines:
        bndbox = []
        size_image=[]
        s=each.split(';')
        try:
            im = Image.open(filePath + s[0])
            im.save(saveFilePath + s[0].replace('ppm', 'png'))
            size_image.append({'width':int(im.width),'height':int(im.height)})

            bndbox.append({'bndbox':[int(s[1]),int(s[2]),int(s[3]),int(s[4])],'object':s[5]})
            data.append({'path':s[0].replace('ppm','png'),'size':size_image[0],'objects':bndbox})
        except:
            pass
    return data

def create_csv_from_xml(data,path):
    with open(path, 'w', newline='') as csvfile:
        writer=csv.writer(csvfile,delimiter=',')
        for data_ in data:
            for objects in data_['objects']:
                print(data_['size']['width'],data_['size']['height'],objects['bndbox'][0],
                      objects['bndbox'][1],objects['bndbox'][2],objects['bndbox'][3],
                      objects['object'],data_['path'])
                writer.writerow([data_['size']['width'],data_['size']['height'],objects['bndbox'][0],
                      objects['bndbox'][1],objects['bndbox'][2],objects['bndbox'][3],
                      objects['object'],data_['path']])

def create_csv(data,path):
    with open(path, 'w', newline='') as csvfile:
        writer=csv.writer(csvfile,delimiter=',')
        for data_ in data:
            #for objects in data_:
                writer.writerow([data_['Width'],data_['Height'],data_['Roi.X1'],
                      data_['Roi.Y1'],data_['Roi.X2'],data_['Roi.Y2'],
                      data_['ClassId'],data_['Path']])


def reformat_csv(oldPath,newPath,set):
    data=[]
    with open(oldPath,newline='') as csvfile:
        reader=csv.reader(csvfile,delimiter=';')
        for row in reader:
            temp=row[0].split(',')
            print(row)
            data.append({'width':temp[0],'height':temp[1],'x1':temp[2],
                         'y1':temp[3],'x2':temp[4],'y2':temp[5],
                         'classid':temp[6],'path':temp[7]})
    print(data)
    with open(newPath, 'w', newline='') as csvfile:
        writer=csv.writer(csvfile,delimiter=',')
        for data_ in data:
            writer.writerow([set,data_['path'].replace('png','jpg'),data_['classid'],
                             round(int(data_['x1'])/int(data_['width']),4),round(int(data_['y1'])/int(data_['height']),4),'','',
                             round(int(data_['x2'])/int(data_['width']),4),round(int(data_['y2'])/int(data_['height']),4),'',''])
def png2jpg(directoryPath,imagesPath):
    data=load('',directoryPath)
    print(data)
    for i in data:
        print(i)
        save_path = imagesPath+i['Path'].replace('png', 'jpg')
        print(save_path)
        try:
            image=Image.open(imagesPath+i['Path'])
            image1 = image.convert('RGB')
            os.remove(imagesPath+i['Path'])
            image1.save(save_path)
        except:pass

def ppm2jpg(directoryPath,imagesPath):
    data=load('',directoryPath)
    for i in data:
        print(i)
        save_path = imagesPath+i['Path'].replace('ppm', 'jpg')
        print(save_path)
        try:
            image=Image.open(imagesPath+i['Path'])
            image1 = image.convert('RGB')
            #os.remove(imagesPath+i['Path'])
            image1.save(save_path)
        except:pass




def compare_csv(trainDirPath,testDirPath,compareDirPath):
    reader=[]
    with open(trainDirPath,newline='') as csvfile:
        for row in csv.reader(csvfile,delimiter=' '):
            reader.append(row)
    with open(testDirPath, newline='') as csvfile:
        for row in csv.reader(csvfile, delimiter=' '):
            reader.append(row)
    with open(compareDirPath, 'w', newline='') as csvfile:
        writer=csv.writer(csvfile,delimiter=' ')
        for i in reader:
            temp=i[0]
            writer.writerow([temp])

PATH_to_directory='prepared_dataset/Road_Sign_Detection/TrainAndTest.csv'
PATH_to_images='image_dataset/Road_Sign_Detection/'

#prepare dataset:

data=load_xml(PATH_to_images)
create_csv(data,PATH_to_directory)
png2jpg(PATH_to_directory,PATH_to_images)
reformat_csv(PATH_to_directory,PATH_to_directory,'TRAIN')

#create model:
spec=model_spec.get("efficientdet_lite0")

#Load data
data=object_detector.DataLoader.from_csv(PATH_to_directory,images_dir='image_dataset/Road_Sign_Detection')
#Create model
model = object_detector.create(train_data=data[0], model_spec=spec, epochs=10, batch_size=8, train_whole_model=True)
#Get metric of model and test model
print(model.evaluate(data[2]))
#Save model
model.export(export_dir='.')
#Get metric of model and test model after quantization
print(model.evaluate_tflite('model.tflite', data[2]))




















# PATH_to_directory='prepared_dataset/GTSRB/Train.csv'
# PATH_to_images='image_dataset/GTSRB/'

#prepare dataset:

# data=load(PATH_to_images+'/','Train.csv')
# print(data[0])
#create_csv(data,PATH_to_directory)
#png2jpg(PATH_to_directory,PATH_to_images)
#reformat_csv(PATH_to_directory,PATH_to_directory,'TRAIN')

#create model:
# spec=model_spec.get("efficientdet_lite0")
#
# data=object_detector.DataLoader.from_csv('prepared_dataset/GTSRB/Comp.csv',images_dir='image_dataset/GTSRB')

# model = object_detector.create(train_data=data[0], model_spec=spec, epochs=1, batch_size=8, train_whole_model=True)
# model.export(export_dir='.')
#------------------------------

#prepare dataset:

# PATH_to_directory='prepared_dataset/GTSDB/Train.csv'
# PATH_to_images='image_dataset/GTSDB/'

#data=load(PATH_to_images+'/','gt.txt',separator=';')
#data=add_photo_size(data,PATH_to_images)
#create_csv(data,PATH_to_directory)
#ppm2jpg(PATH_to_directory,PATH_to_images)
#reformat_csv(PATH_to_directory,PATH_to_directory,'TRAIN')

#create model:
# spec=model_spec.get("efficientdet_lite0")
# #
# data=object_detector.DataLoader.from_csv('prepared_dataset/Train.csv',images_dir='image_dataset/GTSRB')
#
# model = object_detector.create(train_data=data[0],validation_data=data[2] ,model_spec=spec, epochs=20, batch_size=8, train_whole_model=True)
# print(model.evaluate(data[2]))
# model.export(export_dir='.')
#------------------------------

#data=load_txt_and_convert_from_ppm_to_png('image_dataset/GTSDB/','gt.txt','image_dataset/GTSDB/')
#create_csv(data,'image_dataset/GTSDB/Train.csv')
#convertImage('prepared_dataset/Train.csv','prepared_dataset/')
#reformat_csv('prepared_dataset/Train.csv','prepared_dataset/Train.csv','TRAIN')

#data=load('image_dataset/GTSRB/','Train.csv')
#convertImage('prepared_dataset')

#print(data)
#create_csv(data,'prepared_dataset/Train1.csv')




