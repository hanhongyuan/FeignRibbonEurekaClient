package demo.testRibbon;

import java.net.URI;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.client.ClientFactory;
import com.netflix.client.http.HttpRequest;
import com.netflix.client.http.HttpResponse;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;
import com.netflix.niws.client.http.RestClient;

public class RibbonEurekaClientDemo {

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
	
	private static synchronized void initializeRibbonClient() throws Exception {
		ConfigurationManager.loadPropertiesFromResources("sample-client.properties"); // 1
	}
	
	private static synchronized void init() throws Exception {
		ApplicationInfoManager applicationInfoManager = initializeApplicationInfoManager(
				new MyDataCenterInstanceConfig());
		initializeEurekaClient(applicationInfoManager, new DefaultEurekaClientConfig());
		
		initializeRibbonClient();
	}

	public static void main(String[] args) throws Exception {
		init();
		
		RibbonEurekaClientDemo sampleClient = new RibbonEurekaClientDemo();
		sampleClient.testRibbon();
	}

	public void testRibbon() {
		try {
			RestClient client = (RestClient) ClientFactory.getNamedClient("sample-client"); // 2
			HttpRequest request = HttpRequest.newBuilder().uri(new URI("/v1/catalog")).build(); // 3
			for (int i = 0; i < 2; i++) {
				HttpResponse response = client.executeWithLoadBalancer(request); // 4
				System.out.println("Status code for " + response.getRequestedURI() + "  :" + response.getStatus());
			}
			@SuppressWarnings("rawtypes")
			ZoneAwareLoadBalancer lb = (ZoneAwareLoadBalancer) client.getLoadBalancer();
			System.out.println(lb.getLoadBalancerStats());
			ConfigurationManager.getConfigInstance().setProperty("sample-client.ribbon.listOfServers",
					"www.linkedin.com:80,www.google.com:80"); // 5
			System.out.println("changing servers ...");
			Thread.sleep(3000); // 6
			for (int i = 0; i < 2; i++) {
				HttpResponse response = null;
				try {
					response = client.executeWithLoadBalancer(request);
					System.out.println("Status code for " + response.getRequestedURI() + "  : " + response.getStatus());
				} finally {
					if (response != null) {
						response.close();
					}
				}
			}
			System.out.println(lb.getLoadBalancerStats()); // 7
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
