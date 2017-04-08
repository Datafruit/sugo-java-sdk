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

- `FileSender`   
将数据保存到指定文件下，可选择按大小或者按时间保存（daily 参数设为 true 则是按时间保存）。   

若是按大小，文件达到指定大小时，会自动更名。可以配置文件的最大容量和文件的个数。   
例如配置的滚动文件名为 message，文件最大为 10KB，最大数量是 100。   
当 message 达到 10KB 时，会自动更名为 message.1,message.2,message.3 ……………… 直到 message.100。   
（旧数据被往上顶，后缀数字越大的文件，其数据越老）   

若是按时间，SDK 会自动将数据按指定的 dataPattern 保存，生成文件的频率为 dataPattern 中最小的时间单位。   
例如配置的滚动文件名为 message， dataPattern 为 yyyyMMdd'T'HHmm    
则文件名为 message_20170401T2035   


- `ConsoleSender`  
将发送的数据直接输出到控制台   


- `HttpSender`  
将数据发送到指定终端   


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

[{"event":"testEventName","properties":{"distinct_id":"your distinct id","time":1490792691,"sugo_lib":"jdk","token":"project token","key":"value"}}]

```  

其中，单条数据格式：   
```json   
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
```