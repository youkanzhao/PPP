import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.cookie.BestMatchSpecFactory;
import org.apache.http.impl.cookie.BrowserCompatSpecFactory;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Workbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class DataUtil {
	private static String LOGIN_URL = "http://www.bridata.com/user/login/submit";
	private static String DATA_URL = "http://www.bridata.com/front/projects/list";
	private static String CITY_DATA_URL = "http://www.bridata.com/front/citys/list";
	
	

	private static String TARGET_DATA_PATH;

	private static CookieStore cookieStore = null;
	private static HttpClientContext context = null;

	private static List<DataBean> PPP_DATA = new ArrayList<DataBean>();
	
	static {
		String basePath = DataUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		if(basePath.endsWith("jar")) {
			basePath = basePath.replaceAll("PPP\\.jar", "");
			TARGET_DATA_PATH = basePath;
		}
		else {
			TARGET_DATA_PATH = DataUtil.class.getResource("/").getPath();
		}
	}
	
	
	public static void start() throws Exception {
		CloseableHttpClient client = HttpClients.createDefault();

		HttpPost httpPost = new HttpPost(LOGIN_URL);
		Map parameterMap = new HashMap();
		parameterMap.put("mobile", "18902845151");
		parameterMap.put("password", "sztechand");
		UrlEncodedFormEntity postEntity = new UrlEncodedFormEntity(getParam(parameterMap), "UTF-8");
		httpPost.setEntity(postEntity);
		try {
			// 执行post请求
			HttpResponse httpResponse = null;
			try {
				httpResponse = client.execute(httpPost);
			}
			catch(Exception e) {
				System.out.println("失败重试--1");
				httpResponse = client.execute(httpPost);
			}
			
			if(httpResponse == null) {
				System.out.println("失败重试--2");
				httpResponse = client.execute(httpPost);
			}
			
			getResponseContent(httpResponse);
			// cookie store
			setCookieStore(httpResponse);
			// context
			setContext();
			
			getPPPData(client);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				// 关闭流并释放资源
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void getInitData(JSONArray cityJSON) {
		try {
			
			File folder = new File(TARGET_DATA_PATH);
			if(folder.isDirectory()) {
				String[] files = folder.list();
				for(String fileName : files) {
					if(fileName.endsWith("xls")){
						String excelFilePath = TARGET_DATA_PATH + fileName;
						File file = new File(excelFilePath);
						POIFSFileSystem poifsFileSystem = new POIFSFileSystem(new FileInputStream(file));
					    HSSFWorkbook hssfWorkbook =  new HSSFWorkbook(poifsFileSystem);
					    HSSFSheet hssfSheet = hssfWorkbook.getSheetAt(0);

					    int rowstart = 1;
					    int rowEnd = hssfSheet.getLastRowNum();
					    for(int i=rowstart;i<=rowEnd;i++)
					    {
					        HSSFRow row = hssfSheet.getRow(i);
					        DataBean bean = new DataBean();
					        bean.setProjectName(row.getCell(0).getStringCellValue());
					        bean.setArea(row.getCell(1).getStringCellValue());
					        bean.setTrade(row.getCell(2).getStringCellValue());
					        bean.setCapital(row.getCell(3).getStringCellValue());
					        bean.setProgress(row.getCell(4).getStringCellValue());
					        bean.setTime(row.getCell(5).getStringCellValue());
					        
					        int len = cityJSON.size();
							for (int j = 0; j < len; j++) {
								JSONObject json = cityJSON.getJSONObject(j);
								String cityName = json.getString("full_name");
								if(bean.getArea().contains(cityName)) {
									String cityCode = json.getString("code");
									bean.setCityCode(cityCode);
								}
								
							}
					        
					        PPP_DATA.add(bean);
					    }
					    filterData();
					}
				}
			}
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	public static List<NameValuePair> getParam(Map parameterMap) {
		List<NameValuePair> param = new ArrayList<NameValuePair>();
		Iterator it = parameterMap.entrySet().iterator();
		while (it.hasNext()) {
			Entry parmEntry = (Entry) it.next();
			param.add(new BasicNameValuePair((String) parmEntry.getKey(), (String) parmEntry.getValue()));
		}
		return param;
	}

	public static String getResponseContent(HttpResponse httpResponse) throws ParseException, IOException {
		// 获取响应消息实体
		HttpEntity entity = httpResponse.getEntity();
		// 响应状态
		// 判断响应实体是否为空
		if (entity != null) {
			String responseString = EntityUtils.toString(entity);
			return responseString;
		}
		return null;
	}

	public static void setContext() {
		// System.out.println("----setContext");
		context = HttpClientContext.create();
		Registry<CookieSpecProvider> registry = RegistryBuilder.<CookieSpecProvider>create()
				.register(CookieSpecs.BEST_MATCH, new BestMatchSpecFactory())
				.register(CookieSpecs.BROWSER_COMPATIBILITY, new BrowserCompatSpecFactory()).build();
		context.setCookieSpecRegistry(registry);
		context.setCookieStore(cookieStore);
	}

	public static void setCookieStore(HttpResponse httpResponse) {
		// System.out.println("----setCookieStore");
		cookieStore = new BasicCookieStore();
		// JSESSIONID
		String setCookie = httpResponse.getFirstHeader("Set-Cookie").getValue();
		String JSESSIONID = setCookie.substring("JSESSIONID=".length(), setCookie.indexOf(";"));
		// System.out.println("JSESSIONID:" + JSESSIONID);
		// 新建一个Cookie
		BasicClientCookie cookie = new BasicClientCookie("JSESSIONID", JSESSIONID);
		cookie.setVersion(0);
		cookie.setDomain("www.bridata.com");
		cookieStore.addCookie(cookie);
	}

	public static void getPPPData(CloseableHttpClient client) {
		try {
			JSONArray cityJSON = getCityJSONData(client);
			getInitData(cityJSON);
			int len = cityJSON.size();
			for (int i = 0; i < len; i++) {
				JSONObject json = cityJSON.getJSONObject(i);
				String cityNmae = json.getString("full_name");
				String cityCode = json.getString("code");
				String proviceCode = json.getString("provinceCode");
				System.out.println("城市总数：" + len + "  当前编号：" + (i+1));
				getCityData(cityCode, proviceCode, cityNmae, "4", client);
			}
			filterData();
			writeToExcel();
			System.out.println("运行成功！");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static JSONArray getCityJSONData(CloseableHttpClient client) {
		JSONArray result = new JSONArray();
		HttpGet httpGet = new HttpGet(DATA_URL);
		try {
			HttpResponse httpResponse = client.execute(httpGet);
			String html = getResponseContent(httpResponse);
//			System.out.println(html);
			Document doc = Jsoup.parse(html);
			Elements dataElList = doc.select("td#province_btns_td button");
			Iterator it = dataElList.iterator();
			while (it.hasNext()) {
				Element buttonEl = (Element) it.next();
				String onclickStr = buttonEl.attr("onclick");
				String[] tempArr = onclickStr.split(",|\\)|'");
				String provinceCode = tempArr[2];
				String provinceName = tempArr[5];
				// 执行post请求
				HttpPost httpPost = new HttpPost(CITY_DATA_URL);
				Map parameterMap = new HashMap();
				parameterMap.put("province_code", provinceCode);
				UrlEncodedFormEntity postEntity = new UrlEncodedFormEntity(getParam(parameterMap), "UTF-8");
				httpPost.setEntity(postEntity);
				
				//失败重试
				httpResponse = null;
				try {
					httpResponse = client.execute(httpPost);
					JSONArray citysJSON = null;
					if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {  
		                String resData = EntityUtils.toString(httpResponse.getEntity());   
		                citysJSON = JSONArray.fromObject(resData); 
		                int size = citysJSON.size();
						for(int i = 0; i < size; i++) {
							JSONObject  cell = citysJSON.getJSONObject(i);
							cell.put("provinceCode", provinceCode);
							cell.put("provinceName", provinceName);
							result.add(cell);
						}
		            }  
					
				}
				catch(Exception e) {
					System.out.println("失败重试--1");
					httpResponse = client.execute(httpPost);
				}
				
			}
		} catch (IOException e) {
		    e.printStackTrace();
		}
		return result;
	}

	private static void filterData() {
		Collections.sort(PPP_DATA, new Comparator<DataBean>() {
			@Override
			public int compare(DataBean b1, DataBean b2) {
				return b1.getCityCode().compareTo(b2.getCityCode());
			}

		});

		List<DataBean> tempData = new ArrayList<DataBean>();
		for (DataBean bean : PPP_DATA) {
			if (!isDuplicate(bean, tempData)) {
				tempData.add(bean);
			}
		}
		PPP_DATA = tempData;
	}

	private static boolean isDuplicate(DataBean bean, List<DataBean> tempData) {
		for (DataBean db : tempData) {
			if (db.getProjectName().equals(bean.getProjectName()) && db.getArea().equals(bean.getArea())
					&& db.getCapital().equals(bean.getCapital()) && db.getProgress().equals(bean.getProgress())) {
				return true;
			}
		}
		return false;
	}

	private static void getCityData(String city_code_val, String province_code_val, String cityName, String project_state_id_val,
			CloseableHttpClient client) {
		try {
			// 执行post请求
			HttpPost httpPost = new HttpPost(DATA_URL);
			Map parameterMap = new HashMap();
			parameterMap.put("city_code_val", city_code_val);
			parameterMap.put("province_code_val", province_code_val);
			parameterMap.put("project_state_id_val", project_state_id_val);
			UrlEncodedFormEntity postEntity = new UrlEncodedFormEntity(getParam(parameterMap), "UTF-8");
			httpPost.setEntity(postEntity);
			
			//失败重试
			HttpResponse httpResponse = null;
			try {
				httpResponse = client.execute(httpPost);
			}
			catch(Exception e) {
				System.out.println("失败重试--1");
				httpResponse = client.execute(httpPost);
			}
			
			if(httpResponse == null) {
				System.out.println("失败重试--2");
				httpResponse = client.execute(httpPost);
			}
			
			String html = getResponseContent(httpResponse);
			boolean result = parseHtml(html, city_code_val, cityName);
			if (result) {
				int index = 1;
				while (true) {
					index++;
					System.out.println("index==" + index);
					parameterMap.put("page", index + "");
					parameterMap.put("industry_id_val", "-1");
					parameterMap.put("op_type_id_val", "-1");
					parameterMap.put("project_return_id_val", "-1");
					parameterMap.put("start_by_id_val", "-1");
					parameterMap.put("project_batch_id_val", "-1");
					parameterMap.put("year", "-1");
					parameterMap.put("is_turn_page", "1");
					parameterMap.put("hide_sel_content_trs_val", "1");
					parameterMap.put("amount_min", "0");
					parameterMap.put("amount_max", "0");
					parameterMap.put("duration_min", "0");
					parameterMap.put("duration_max", "0");

					postEntity = new UrlEncodedFormEntity(getParam(parameterMap), "UTF-8");
					httpPost.setEntity(postEntity);
					
					httpResponse = null;
					try {
						httpResponse = client.execute(httpPost);
					}
					catch(Exception e) {
						System.out.println("失败重试--1");
						httpResponse = client.execute(httpPost);
					}
					
					if(httpResponse == null) {
						System.out.println("失败重试--2");
						httpResponse = client.execute(httpPost);
					}
					
					html = getResponseContent(httpResponse);
					result = parseHtml(html, city_code_val, cityName);
					Thread.sleep(500);
					if (!result) {
						break;
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void writeToExcel() {
		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		String excelFilePath = TARGET_DATA_PATH + "data_" + sdf.format(d) + ".xls";
//		System.out.println(excelFilePath);
		ExcelUtil.writeExcel(PPP_DATA, excelFilePath);
	}

	private static boolean parseHtml(String html, String cityCode, String cityNmae) {
		boolean result = false;
		try {
//			System.out.println(html);
			Document doc = Jsoup.parse(html);
			Element tableEl = doc.select("table.msgtable").first();
			Elements trEls = tableEl.select("tr");
			Iterator it = trEls.iterator();
			while (it.hasNext()) {
				Element trEl = (Element) it.next();
				Elements tdEls = trEl.select("td");
				if (tdEls.size() == 6) {
					DataBean dataBean = new DataBean();
					String projectName = tdEls.get(0).text();
					String area = tdEls.get(1).text();
					String trade = tdEls.get(2).text();
					String capital = tdEls.get(3).text();
					String progress = tdEls.get(4).text();
					String time = tdEls.get(5).text();

					dataBean.setProjectName(projectName);
					dataBean.setArea(area);
					dataBean.setTrade(trade);
					dataBean.setCapital(capital);
					dataBean.setProgress(progress);
					dataBean.setTime(time);
					dataBean.setCityCode(cityCode);
					PPP_DATA.add(dataBean);
					result = true;
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("数据数目：" + PPP_DATA.size() + "  城市：" + cityNmae);
		return result;
	}

	public static void main(String[] args) throws Exception {
		start();
	}

}
