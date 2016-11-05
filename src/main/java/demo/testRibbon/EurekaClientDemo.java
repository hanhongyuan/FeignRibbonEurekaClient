package demo.testRibbon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.List;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;

/**
 * Hello world!
 *
 */
public class EurekaClientDemo {

	private static ApplicationInfoManager applicationInfoManager;
	private static EurekaClient eurekaClient;

	private static synchronized ApplicationInfoManager initializeApplicationInfoManager(
			EurekaInstanceConfig instanceConfig) {
		if (applicationInfoManager == null) {
			InstanceInfo instanceInfo = new EurekaConfigBasedInstanceInfoProvider(instanceConfig).get();
			applicationInfoManager = new ApplicationInfoManager(instanceConfig, instanceInfo);
		}

		return applicationInfoManager;
	}

	private static synchronized EurekaClient initializeEurekaClient(ApplicationInfoManager applicationInfoManager,
			EurekaClientConfig clientConfig) {
		if (eurekaClient == null) {
			eurekaClient = new DiscoveryClient(applicationInfoManager, clientConfig);
		}

		return eurekaClient;
	}

	public void sendRequestToServiceUsingEureka(EurekaClient eurekaClient) {
		// initialize the client
		// this is the vip address for the example service to talk to as defined
		// in conf/sample-eureka-service.properties
		String vipAddress = "catalog-service";

		InstanceInfo nextServerInfo = null;
		try {
			nextServerInfo = eurekaClient.getNextServerFromEureka(vipAddress, false);
		} catch (Exception e) {
			System.err.println("Cannot get an instance of example service to talk to from eureka");
			System.exit(-1);
		}

		System.out.println("Found an instance of example service to talk to from eureka: "
				+ nextServerInfo.getVIPAddress() + ":" + nextServerInfo.getPort());

		System.out.println("healthCheckUrl: " + nextServerInfo.getHealthCheckUrl());
		System.out.println("override: " + nextServerInfo.getOverriddenStatus());

		Socket s = new Socket();
		int serverPort = nextServerInfo.getPort();
		try {
			s.connect(new InetSocketAddress(nextServerInfo.getHostName(), serverPort));
		} catch (IOException e) {
			System.err.println(
					"Could not connect to the server :" + nextServerInfo.getHostName() + " at port " + serverPort);
		} catch (Exception e) {
			System.err.println("Could not connect to the server :" + nextServerInfo.getHostName() + " at port "
					+ serverPort + "due to Exception " + e);
		}
		try {
			String request = "FOO " + new Date();
			System.out.println("Connected to server. Sending a sample request: " + request);

			PrintStream out = new PrintStream(s.getOutputStream());
			out.println(request);

			System.out.println("Waiting for server response..");
			BufferedReader rd = new BufferedReader(new InputStreamReader(s.getInputStream()));
			String str = rd.readLine();
			if (str != null) {
				System.out.println("Received response from server: " + str);
				System.out.println("Exiting the client. Demo over..");
			}
			rd.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void getServersFromEureka(EurekaClient eurekaClient){
		String vipAddress = "catalog-service";

		List<InstanceInfo> instanceInfoList = null;
		try {
			instanceInfoList = eurekaClient.getInstancesByVipAddress(vipAddress, false);
		} catch (Exception e) {
			System.err.println("Cannot get an instance of example service to talk to from eureka");
			System.exit(-1);
		}

		for(InstanceInfo nextServerInfo:instanceInfoList){
			System.out.println("Found an instance of example service to talk to from eureka: "
					+ nextServerInfo.getVIPAddress() + ":" + nextServerInfo.getPort());

			System.out.println("healthCheckUrl: " + nextServerInfo.getHealthCheckUrl());
			System.out.println("override: " + nextServerInfo.getOverriddenStatus());
		}
		
	}

	public static void main(String[] args) {
		EurekaClientDemo sampleClient = new EurekaClientDemo();

		// create the client
		ApplicationInfoManager applicationInfoManager = initializeApplicationInfoManager(
				new MyDataCenterInstanceConfig());
		EurekaClient client = initializeEurekaClient(applicationInfoManager, new DefaultEurekaClientConfig());

		// use the client
//		sampleClient.sendRequestToServiceUsingEureka(client);
		
		
		sampleClient.getServersFromEureka(client);
		

		// shutdown the client
		eurekaClient.shutdown();
	}
}
