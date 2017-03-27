# sugo-java-sdk
Java SDK of Sugo

## 使用  

**1 生成数据** (`MessageBuilder`)  
使用 MessageBuilder.event() 可以创建一条数据  

**2 暂存数据** (`MessagePackage`)  
使用 `MessagePackage.addMessage()` 来暂存生成的数据   
> 也可以不暂存数据，直接发送，但是暂存起来批量发送的效率比逐条发送要高的多，所以强烈推荐暂存数据。  

**3 发送数据** (`SugoAPI`)   
- 发送单条数据（不推荐）--`SugoAPI.sendMessage()`   
- 发送批量数据--`SugoAPI.sendMessages()`  

**4 发送方式** (`Sender`)  
数据可以被发送至终端服务器，也可以保存为文件，这取决于构造`SugoAPI`的构造函数。   

- HttpSender  
- FileSender   
- ConsoleSender  

