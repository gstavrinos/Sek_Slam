#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <math.h>
#include <stdio.h>
#include <ctype.h>
#include <iostream>
#include <stdio.h>
#include <unistd.h>
#include <ros/ros.h>
#include <sensor_msgs/Image.h>
#include <sensor_msgs/image_encodings.h>
#include <signal.h>
#include <phidget21.h>
#include <std_msgs/Float64.h>
#include "servo_mast/mast_position.h"
#include "servo_mast/mast_turn.h"
#include <sys/time.h>
#include <chrono>
#include <ctime>
#include <boost/bind.hpp>
#include <boost/ref.hpp>
#include <image_transport/image_transport.h>
#include <cv_bridge/cv_bridge.h>
#include "opencv2/objdetect/objdetect.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "cv.h"
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/calib3d/calib3d.hpp>

using namespace cv;

int IMAGE_RECEIVED = 0;
int FACE_DETECTED = 0;
sensor_msgs::Image gl_image;
double tilt;
void detect_face();

void imageCallback(const sensor_msgs::Image::ConstPtr& str_img){
	if (IMAGE_RECEIVED == 0){
		gl_image.height = str_img->height;
		gl_image.width = str_img->width;
       		gl_image.encoding = str_img->encoding;
        	gl_image.step = str_img->step;
        	gl_image.data.clear();
        	gl_image.data = str_img->data;

		IMAGE_RECEIVED = 1;
	}

	detect_face();
}

void tiltCallback(const std_msgs::Float64::ConstPtr& msg){
	tilt = msg->data;
}

void detect_face(){
	CascadeClassifier face_cascade;
	if( !face_cascade.load("/home/skel/ws2/src/sek_drive/src/haarcascades/haarcascade_frontalface_alt.xml") ){
 		printf("Error loading face_cascade\n"); 
		return;
	}
	//face_cascade = cv2.CascadeClassifier('haarcascade_frontalface_default.xml')
	std::vector<Rect> faces;
    	Mat frame_gray;
	cv_bridge::CvImagePtr frame;
	float max_width = 0.0;
	float max_height = 0.0;
	int closestFace = 0;
	try{
		frame = cv_bridge::toCvCopy(gl_image);
    		cvtColor( frame->image, frame_gray, COLOR_BGR2GRAY );
	    	equalizeHist( frame_gray, frame_gray );
		face_cascade.detectMultiScale( frame_gray, faces, 1.1, 2, 0|CASCADE_SCALE_IMAGE, Size(30, 30) );
		for( size_t i = 0; i < faces.size(); i++ ){
			if(max_width < faces[i].width && max_height <  faces[i].height){
				closestFace = i;
			}
		}
		if(faces.size() > 0){
            FACE_DETECTED = 1;
			Point center( faces[closestFace].x + faces[closestFace].width/2, faces[closestFace].y + faces[closestFace].height/2 );
                        ellipse( frame->image, center, Size( faces[closestFace].width/2, faces[closestFace].height/2), 0, 0, 360, Scalar( 255, 0, 0), 4, 8, 0 );
	                if(abs(frame->image.rows/2 - center.x) > 10 && abs(frame->image.cols/2 - center.y) > 10){
				
				if(abs(frame->image.rows/2 - center.x) > 10){
					if(frame->image.rows/2 - center.x < 0){//the face is on the right
						//TODO rotate 10 to the right
                        
					}
					else{
						//TODO rotate 10 to the left
					}
				}
				if(abs(frame->image.cols/2 - center.y) > 10){
					if(frame->image.cols/2 - center.y > 0){//the face is up
                        			tilt += 0.0666;
					}
					else{
                        			tilt -= 0.0666;
					}
				}

        	        }
		}
        else
        {
            FACE_DETECTED=0;
        }
	}
	catch(cv_bridge::Exception& e){
		ROS_ERROR("cv_bridge exception: %s", e.what());
	}
	
	sensor_msgs::ImagePtr msg = frame->toImageMsg();
        gl_image.height = msg->height;
        gl_image.width = msg->width;
        gl_image.encoding = msg->encoding;
        gl_image.step = msg->step;
        gl_image.data.clear();
        gl_image.data = msg->data;


}

int main(int argc, char** argv){
	ros::init(argc, argv, "face_detection");
	ros::NodeHandle n;

	sensor_msgs::Image image;
    
	ros::Subscriber img_in;
	img_in = n.subscribe<sensor_msgs::Image>("/camera/rgb/image_color", 5, imageCallback);
    
    ros::Subscriber cur_tilt;
	cur_tilt = n.subscribe("cur_tilt", 5, tiltCallback);
    
	ros::Publisher face_det;
	face_det = n.advertise<sensor_msgs::Image>("/face_detection", 100);
    
    ros::Publisher tilt_pub;
	tilt_pub = n.advertise<std_msgs::Float64>("/tilt_angle", 10);
    std_msgs::Float64 msg_ ;
	while(ros::ok()){
		if(IMAGE_RECEIVED == 1){
			face_det.publish(gl_image);
			IMAGE_RECEIVED = 0;
		}
        if (FACE_DETECTED == 1){
            msg_.data = tilt;
            tilt_pub.publish(msg_);
        }
		ros::spinOnce();
	}
	return 0;
}
