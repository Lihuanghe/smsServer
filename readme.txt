支持CMPP ,SMPP ,SMGP,SGIP四种协议

1: 修改  server.xml 配置文件，支持多个server 端口，每个server支持多个账号。
		maxChannels 配置可限制单个账号的连接数。
		
2：日志级别修改
	修改logback.xml配置文件。
	<logger name="entity" additivity="false">
		<level value="trace" />
		<appender-ref ref="SIFT" />
	</logger>
	trace 级别打印二进制报文 和 ToString化的消息收发记录
	debug级别只打印 ToString化的消息收发记录
	info 级别不打印消息收发记录