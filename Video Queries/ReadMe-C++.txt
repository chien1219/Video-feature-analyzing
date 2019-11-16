NOW - also plays sound along with displaying images

You will find
- A program to display and manipulate images. This has been
given as Microsoft Visual C++ .dsp, .dsw, .vcproj project files along with
the accompanying code in the .cpp and .h files
- Example images, which are in the native RGB formats. They are
at dimension 352x288

The project includes the following important files.
1. Image.h	- header file for MyImage class
2. Image.cpp	- defines operations on an image such as read, write, modify
3. Main.cpp	- entry point of the application, takes care of the GUI and the 
		  image display operations

Some indicators have been placed in the code to guide you to develop your code. But you
you are free to modify the program in any way to get the desirable output.

- Unzip the folder in your desired location
- Launch Visual Studio and load the .dsw or .dsp project files
- If you use the .net compiler, you can still load the project file, just 
  follow the default prompts and it will create a solution .sln file for you
- Compile the program to produce a an executable Image.exe
- To run the program you need to provide the program with command line arguments, they can 
  be passed in Visual C++ using Project > Settings or just launch a .exe file from the command prompt
- Here is the usage (for the given example images)

Image.exe image1.rgb 352 288 sound.wav