# sugo-java-sdk
Java SDK of Sugo

## 使用  

## 1 默认使用  
>  调用者自行维持　sugoAPI 的单例

１　调用构造函数　`SugoAPI(sender)`.   
２　记录　message ，调用　`sugoAPI.event(name,properties);` 即可。   
３　（可选）可以跟据自己的数据量，调整读取并发送数据的线程数　`SugoConfig.DEFAULT_WORKER_CUSTOMER_COUNT`.   
４　（可选）可以调整数据队列的最大长度　`SugoConfig.DEFAULT_WORKER_QUEＵE_CAPACITY`.   
   
[Demo 代码](https://github.com/Datafruit/sugo-java-sdk/blob/master/src/main/java/io/sugo/sugojavasdk/SugoAPIDemo.java)   


**发送方式** (`Sender`)  
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


单条数据格式：   
```json   
{
	"test": "value",
	"time": 1512033285534,
	"event": "TestEvent99"
}
```   
