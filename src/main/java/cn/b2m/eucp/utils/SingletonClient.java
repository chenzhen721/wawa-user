package cn.b2m.eucp.utils;

import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;




public class SingletonClient {
	private static Client client=null;
    public static String softwareSerialNo = "9SDK-EMY-0999-JETSN";// 软件序列号,请通过亿美销售人员获取
    public static String key = "jeBQzrQCxXDLEDycHCsrmNqLDuqLBrD6RcZboTr6ziGbpuVJBm";// 序列号首次激活时自己设定
    public static String password = "230539";// 密码,请通过亿美销售人员获取
	private SingletonClient(){
	}
	public synchronized static Client getClient(String softwareSerialNo,String key){
		if(client==null){
			try {
				client=new Client(softwareSerialNo,key);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return client;
	}
	public synchronized static Client getClient(){
		if(client==null){
			try {
				client=new Client(softwareSerialNo,key);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return client;
	}
	
	
}
