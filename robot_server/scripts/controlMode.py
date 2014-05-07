import numpy
import rospy
import roslib
import os

class controlMode(object):

	debug=True
	def __init__(self,mode):
		rospy.set_param('sek_drive',{'con_mode':int(mode)})
