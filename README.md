# ScopedStorage_CRUD

This project demonstrates how to implement ScopedStorage CRUD on Android.

It gets runtime read and write external storage permission on button clicks.
The first button opens the gallery and displays the chosen picture on the image view.
The second button opens the camera and saves the picture in external storage.
The third button deletes a picture from external storage.
This project is implemented for images only according to the scoped storage context.
It also implements two methods to save pictures, one for Android 10 and greater and the other for Android 9 and lower.

Here are some things to keep in mind when using ScopedStorage:

You can only access files that you have permission to access.
You can only access files that are in your app's sandbox.
You can only access files that are in the correct directory.
You can only access files that are of the correct type.

I hope this helps!
