## 다수의 파일 처리

앞서 작성했던 프로그램은 모두 다, 한번 접속 후 하나의 파일만을 전송하고나서 더 이상 추가의 파일은 전송이 불가했다.  

1. 파일명 전송
2. 파일 내용 전송
   
sender 측에서 위 과정을 통해 하나의 파일을 전송후 스트림을 close() 해버렸기때문이다.
Receiver 는 sender측 스트림이 끊기면 "아, 더이상 전달받을 파일이 없구나" 확인해버렸다.

하지만 이번에 해볼 프로그램은 하나의 파일이 아닌, 다수의 파일들을 순차적으로 게속 전달할 수 있도록해볼 것이다.  

  1. 파일명 전송 (문자열 형태로)
  2. **파일 크기 전송 (숫자 형태로)**
  3. 파일 내용 전송 (바이트 형태로)

위와 같이, 전송하고자하는 파일의 크기를 별도로 전달한다.  

즉 Receiver 입장에서 스트림이 닫힌것을통해 파일전송이 끝났음을 파악하는 것이 아니라,  
"**내가 전달 받을 파일의 크기가 얼마인지를 미리 확인**할 수 있도록하고, 그 크기만큼 파일의 내용을 전달받을 수 있도록해줌으로써 한 파일의 전송이 모두 끝났다라는 것을 Receiver가 명확하게 확인해볼 수 있을 것이다."  

즉, 내가 전달받고자하는 파일의 크기가 1KB라면 1KB크기의 파일내용이 모두 전달이되었을때, 하나의 파일 전송이끝났구나 확인하는것. 

이후 전달되는 내용은, 현재 파일의 내용이 지속되어지는 것이 아니라 => **"새로운 파일에대한 이름이 전달되어지는것을 통해 다수의 파일들이 순차적으로 전달될 수 있도록 해보자"** 
   
