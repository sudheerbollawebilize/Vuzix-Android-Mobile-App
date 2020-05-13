# Vuzix-Android-Mobile-App
This is an example which uses TransferSDK and transfer data over TCP Socket between Vuzix Blade and mobile. 


For a detailed example/implementation, check this file.
https://github.com/sudheerbollawebilize/Vuzix-Android-Mobile-App/blob/9ef1ced019c38afb830f682cd17cc58a157a639d/app/src/main/java/com/webilize/vuzixfilemanager/services/RXConnectionFGService.java#L63

There are three steps in the working of app.

Look out for available wifi services after switching wifi on in your blade app.
Here is the code snippet for implementation:

https://github.com/sudheerbollawebilize/Vuzix-Android-Mobile-App/blob/9ef1ced019c38afb830f682cd17cc58a157a639d/app/src/main/java/com/webilize/vuzixfilemanager/services/RXConnectionFGService.java#L402

Once the scanning is completed, we will get the list of available devices.
https://github.com/sudheerbollawebilize/Vuzix-Android-Mobile-App/blob/9ef1ced019c38afb830f682cd17cc58a157a639d/app/src/main/java/com/webilize/vuzixfilemanager/services/RXConnectionFGService.java#L249

We can select device and initiate connection request.
https://github.com/sudheerbollawebilize/Vuzix-Android-Mobile-App/blob/9ef1ced019c38afb830f682cd17cc58a157a639d/app/src/main/java/com/webilize/vuzixfilemanager/services/RXConnectionFGService.java#L379

It will raise a request in the blade app. Once the request is accepted, it will be received in the callback.
https://github.com/sudheerbollawebilize/Vuzix-Android-Mobile-App/blob/9ef1ced019c38afb830f682cd17cc58a157a639d/app/src/main/java/com/webilize/vuzixfilemanager/services/RXConnectionFGService.java#L276

Now, both the devices are connected and we can request the blade contents based on our requirement or send the files.

1. Sending a file to blade:
https://github.com/sudheerbollawebilize/Vuzix-Android-Mobile-App/blob/9ef1ced019c38afb830f682cd17cc58a157a639d/app/src/main/java/com/webilize/vuzixfilemanager/services/RXConnectionFGService.java#L526
2. Requesting files or folders:
https://github.com/sudheerbollawebilize/Vuzix-Android-Mobile-App/blob/9ef1ced019c38afb830f682cd17cc58a157a639d/app/src/main/java/com/webilize/vuzixfilemanager/services/RXConnectionFGService.java#L440

