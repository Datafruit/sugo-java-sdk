# sugo-java-sdk
Java SDK of Sugo

## 使用  

**1 生成数据** (`MessageBuilder`)  
使用 `MessageBuilder.event()` 可以创建一条数据  

**2 暂存数据** (`MessagePackage`)  
使用 `MessagePackage.addMessage()` 来暂存生成的数据   
> 也可以不暂存数据，直接发送，但是暂存起来批量发送的效率比逐条发送要高的多，所以强烈推荐暂存数据。  

**3 发送数据** (`SugoAPI`)   
- 发送单条数据（不推荐）--`SugoAPI.sendMessage()`   
- 发送批量数据--`SugoAPI.sendMessages()`  

**4 发送方式** (`Sender`)  
数据可以被发送至终端服务器，也可以保存为文件，这取决于`SugoAPI`构造函数的参数。   

- `HttpSender`  
将数据发送到指定终端   


- `FileSender`   
将数据保存到指定目录下，以每天日期生成子目录，目录名规则为 yyyy-MM-dd，   
每次发送数据将在该子目录下生成一个文件，文件名规则为 HH-mm-ss-SSS   


- `ConsoleSender`  
将发送的数据直接输出到控制台   



**输出数据格式**   
例如，调用   
```java   
    // 生产数据
    MessageBuilder messageBuilder = new MessageBuilder("project token");   
    JSONObejct properties = new JSONObejct();
    properties.put("key", "value");
    JSONObject message = messageBuilder.event("your distinct id", "testEventName", properties);   // 得到包装后的 Message  
    
    // 暂存生成的数据   
    MessagePackage messages = new MessagePackage();
    messages.addMessage(message);
    
    // 发送数据   
    mSugoAPI.sendMessages(messages);
    
```   
输出的数据如下：   

```json
[
  {
    "event": "testEventName",
    "properties": {
      "distinct_id": "your distinct id",
      "time": 1490754297,
      "sugo_lib": "jdk",
      "token": "project token",
      "key": "value"
    }
  }
]
```  

