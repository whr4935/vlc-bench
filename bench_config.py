#!/usr/bin/python2.7

import sys
import urllib2
import json
from pprint import pprint
import dbus
import sys
import os
import time
from Xlib import display
import threading
import gtk.gdk
from PIL import Image
from PIL import ImageColor
from shutil import copyfile
import hashlib
from math import pow

class KeyHandler:

    def __init__(self):
        self.display = display.Display()
        self.last_pressed = None

    def fetch_key(self):
        keypresses_raw = self.display.query_keymap()

        pressed = None
        for i, k in enumerate(keypresses_raw):
            if (i == 1 and k == 2):
                print("esc")
                pressed = "<esc>"
            elif (i == 4 and k == 16):
                print("enter")
                pressed = "<enter>"

        if pressed == self.last_pressed:
            pressed = None
            state_changed = False
        else:
            state_changed = True

        return state_changed, pressed

#function for starting vlc in other thread.
def startVlc():
    os.popen("vlc")

class VlcDbus:

    def __init__(self):
        self.thread = threading.Thread(target=startVlc)
        self.thread.start()
        time.sleep(2)
        self.session_bus = dbus.SessionBus()
        self.vlcHandle = self.session_bus.get_object('org.mpris.MediaPlayer2.vlc',
                                                     '/org/mpris/MediaPlayer2')
        #interface handles
        self.vlcPlayer = dbus.Interface(self.vlcHandle, "org.mpris.MediaPlayer2.Player")
        self.vlcProp = dbus.Interface(self.vlcHandle, "org.freedesktop.DBus.Properties")
        self.vlc = dbus.Interface(self.vlcHandle, "org.mpris.MediaPlayer2")
        
    def quit(self):
        self.vlc.Quit()
        
    def play(self):
        self.vlcPlayer.Play()

    def pause(self):
        self.vlcPlayer.Pause()

    def getPosition(self):
        property = self.vlcProp.Get("org.mpris.MediaPlayer2.Player", "Position")
        #result is in microseconds
        #it is needed in milliseconds
        property /= 1000
        return (property)

    def getPlaybackStatus(self):
        property = self.vlcProp.Get("org.mpris.MediaPlayer2.Player", "PlaybackStatus")
        return property
    
    def open(self, uri):
        self.vlcPlayer.OpenUri(uri)

class JsonHandler:

    def __init__(self):
        if (not os.path.exists("./samples")):
            os.mkdir("./samples")
        elif (not os.path.isdir("./samples")):
            os.remove("./samples")
            os.mkdir("./samples")
        if (not os.path.exists("./samples/config.json")):
            self.jsonfile = open("./samples/config.json", "w+")
        else:
            self.jsonfile = open("./samples/config.json", "r+")

    def getOnlineJsonObject(self):
        jsonstr = self.jsonfile.read()
        jsondata = json.loads(jsonstr)
        return (jsondata)

    def getJsonObject(self):
        jsonstr = self.jsonfile.read()
        #print("jsonstr = " + jsonstr)
        if (jsonstr == ""):
            return []
        else:
            jsondata = json.loads(jsonstr)
            return (jsondata)

    def printJsonObject(self, jsonObject):
        for unit in jsonObject:
            pprint(unit)
            print

    def setJsonObject(self, data):
        datastr = json.dumps(data, indent=4, separators=(',', ': '))
        self.jsonfile.seek(0)
        self.jsonfile.write(datastr)
        self.jsonfile.truncate()

    def closeJsonObject(self):
        self.jsonfile.close()


def binprint(intvalue):
    binstr = "{0:b}".format(intvalue)
    #print("binstr = " + binstr)
    print("len binstr = " + str(len(binstr)))
    tab = []
    for i in range (0, len(binstr)):
        #print("binstr[" + str(i) + "] = " + str(binstr[i]))
        tab.append(binstr[i])
        if (i > 0 and i % 8 == 7):
            print tab
            tab = []

def getIntFromRGB(rgb):
    red = rgb[0]
    green = rgb[1]
    blue = rgb[2]
    RGBint = int(255 << 24) | int(red << 16) | int(green << 8) | int(blue)
    #RGBint = (red<<16) + (green<<8) + blue
    #RGBint = int(blue << 16) | int(green << 8) | int(red)
    binprint(RGBint)
    RGBint = int(RGBint - pow(2, 32))
    print ("RBG = {0:b}".format(RGBint))
    return RGBint

def getColorValue(i):
    window = gtk.gdk.get_default_root_window()
    size = window.get_size()
    pixelBuf = gtk.gdk.Pixbuf(gtk.gdk.COLORSPACE_RGB, False, 8, size[0], size[1])
    pixelBuf = pixelBuf.get_from_drawable(window, window.get_colormap(),
                                          0, 0, 0, 0, size[0], size[1])
    red = 0
    green = 0
    blue = 0
        
    if (pixelBuf != None):
        filename = "screenshot.png"
        pixelBuf.save(filename, "png")
        image = Image.open(filename) #Can be many different formats.
        pix = image.load()
        imageSize = image.size
        for i in range(0, imageSize[0]):
            for inc in range(0, imageSize[1]):
                red += pix[i, inc][0]
                green += pix[i, inc][1]
                blue += pix[i, inc][2]
        red /= imageSize[0] * imageSize[1]
        green /= imageSize[0] * imageSize[1]
        blue /= imageSize[0] * imageSize[1] 
        rgbcolor = getIntFromRGB([red, green, blue])

        print("red = " + str(red))
        print("green = " + str(green))
        print("blue = " + str(blue))
        print("rgb = " + str(rgbcolor))

        os.remove(filename)
        return rgbcolor    
    else:
        print("Failed to get screenshot")
    return 0
        
def selectionLoop(filepath):
    vlc = VlcDbus()
    vlc.open("file://" + filepath)
    keyHandle = KeyHandler()
    tab = list()
    i = 0
    while vlc.getPlaybackStatus() != "Stopped":
        state_changed, pressed = keyHandle.fetch_key()
        if (state_changed and pressed == "<enter>"):
            vlc.pause()
            position = vlc.getPosition()
            colorValue = getColorValue(i)
            print ("position: " + str(vlc.getPosition()) + " ms")
            tab.append(list([position, colorValue]))
            i += 1
            vlc.play()
        if (state_changed and pressed == "<esc>" and len(tab) >= 2):
            break
    vlc.quit()
    return tab

def getChecksum(filepath):
    filestr = open(filepath, "r")
    hexhash = hashlib.sha512(filestr.read()).hexdigest()
    print ("Checksum = " + hexhash)
    return hexhash

#gets the name at the end of the given path
def getFileName(filepath):
    tab = filepath.split("/")
    return tab[len(tab) - 1]

def edit(filepath):
    print ("edit option was not yet implemented")
    
def add(filepath):
    if (not os.path.exists(filepath)):
        print ("File not found")
        return
    name = getFileName(filepath)
    if (find(name)):
        print ("That sample already exists")
    else:
        sample = dict()
        sample["snapshot"] = selectionLoop(filepath)
        sample["name"] = name
        sample["url"] = name
        sample["checksum"] = getChecksum(filepath)
        
        jHandler = JsonHandler()
        jsonData = jHandler.getJsonObject()
        jsonData.append(sample)
        jHandler.setJsonObject(jsonData)
        jHandler.closeJsonObject()
        copyfile(filepath, "./samples/" + name)

def delete(filepath):
    jHandle = JsonHandler()
    jsonData = jHandle.getJsonObject()
    name = getFileName(filepath)
    obj = None
    for element in jsonData:
        if (element["name"] == name) : obj = element
    if (obj != None) :
        jsonData.remove(obj)
        if (not os.path.exists("./samples/" + name)):
            print ("File not found")
            return
        os.remove("./samples/" + getFileName(filepath))
        print ("Deleted " + filepath)
    else:
        print ("Could not find the json object to delete")
    jHandle.setJsonObject(jsonData)
    jHandle.closeJsonObject()

def find(filepath):
    jHandle = JsonHandler()
    jsonData = jHandle.getJsonObject()
    for element in jsonData:
        if (element["name"] == getFileName(filepath)):
            return True
    return False
    
def show():
    jHandle = JsonHandler()
    jsonData = jHandle.getJsonObject()
    jHandle.closeJsonObject()
    i = 0
    for element in jsonData:
        print(str(i) + " - " + element["name"])
        i += 1

def showJson():
    jHandle = JsonHandler()
    jsonData = jHandle.getJsonObject()
    jHandle.printJsonObject(jsonData)
    jHandle.closeJsonObject()

def updateFileName(filename):
    if (filename.find("/") == -1):
        filename = os.getcwd() + "/" + filename
    elif (filename[0] == '.' and filename[1] == '/'):
        filename = os.getcwd() + "/" + filename[2:]
    return filename

def reminders():
# reminders
    c = 0
    while (c != 'y' and c != 'n' and c != '\n'): 
        c = raw_input("Have you activated the vlc option " +
                  "Tools/Preferences/Video/Display/Fullscreen ? [y/N]: ")
    if (c != 'y'):
        exit(1)
    c = 0
    while (c != 'y' and c != 'n' and c != '\n'): 
        c = raw_input("Have you synchronised the folder with the rsync folder ? [y/N]: ")
    if (c != 'y'):
        exit(1)
    

usage = "Usage: ./bench_config.py --[add | del | edit | list | json] [filepath]"
# option selection
if (len(sys.argv) == 2):
    option = sys.argv[1]
    if (option == "--list" or option == "-l"):
        show()
    elif (option == "--json" or option == "-j"):
        showJson()
    else:
        print (usage)
elif (len(sys.argv) == 3):
    option = sys.argv[1]
    filepath = updateFileName(sys.argv[2])
    getFileName(filepath)
    if (option == "--add" or option == "-a"):
        add(filepath)
    elif (option == "--del" or option == "-d"):
        delete(filepath)
    elif (option == "--edit" or option == "-e"):
        print ("To be Implemented")
    else:
        print (usage)
else:
    print (usage)
