import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelUtil {
	private static final String EXCEL_XLS = "xls";
	private static final String EXCEL_XLSX = "xlsx";

	public static void writeExcel(List<DataBean> dataList, String finalXlsxPath) {
		OutputStream out = null;
		try {
			// 获取总列数
			// 读取Excel文档
			File finalXlsxFile = new File(finalXlsxPath);

			Workbook workBook = getWorkbok(finalXlsxFile);
			// sheet 对应一个工作页
			Sheet sheet = workBook.createSheet();
			/**
			 * 删除原有数据，除了属性列
			 */
			int rowNumber = sheet.getLastRowNum(); // 第一行从0开始算
			System.out.println("原始数据总行数，除属性列：" + rowNumber);
			for (int i = 1; i <= rowNumber; i++) {
				Row row = sheet.getRow(i);
				sheet.removeRow(row);
			}
			// 创建文件输出流，输出电子表格：这个必须有，否则你在sheet上做的任何操作都不会有效
			out = new FileOutputStream(finalXlsxPath);
			workBook.write(out);
			/**
			 * 往Excel中写新数据
			 */

			createHeader(workBook, sheet);

			for (int j = 0; j < dataList.size(); j++) {
				// 创建一行：从第二行开始，跳过属性列
				Row row = sheet.createRow(j + 1);
				// 得到要插入的每一条记录
				DataBean dataBean = dataList.get(j);

				for (int k = 0; k <= 5; k++) {
					// 在一行内循环
					
					Cell cell = row.createCell(k);
					String value = "";
					int width = 15000;
					switch (k) {
					case 0:
						value = dataBean.getProjectName();
						 width = 15000;
						break;
					case 1:
						value = dataBean.getArea();
						width = 10000;
						break;
					case 2:
						value = dataBean.getTrade();
						 width = 10000;
						break;
					case 3:
						value = dataBean.getCapital();
						width = 6000;
						break;
					case 4:
						value = dataBean.getProgress();
						width = 6000;
						break;
					case 5:
						value = dataBean.getTime();
						width = 6000;
						break;
					}
					sheet.setColumnWidth(k, width);
					cell.setCellValue(value);

				}
			}
			// 创建文件输出流，准备输出电子表格：这个必须有，否则你在sheet上做的任何操作都不会有效
			out = new FileOutputStream(finalXlsxPath);
			workBook.write(out);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.flush();
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("数据导出成功");
	}

	private static void createHeader(Workbook workBook, Sheet sheet) {
		Row row = sheet.createRow(0);
		// 得到要插入的每一条记录
		CellStyle cellStyle = workBook.createCellStyle();
		// 设置字体
        Font font = workBook.createFont();
        font.setFontHeightInPoints((short) 14); //字体高度
        font.setColor(HSSFFont.COLOR_NORMAL); //字体颜色
        font.setFontName("黑体"); //字体
        font.setItalic(true); //是否使用斜体
//        font.setStrikeout(true); //是否使用划线
        cellStyle.setFont(font);
		for (int k = 0; k <= 5; k++) {
			// 在一行内循环
			
			Cell cell = row.createCell(k);
			cell.setCellStyle(cellStyle);
			String value = "";
			switch (k) {
			case 0:
				value = "项目名字";
				break;
			case 1:
				value = "地区";
				break;
			case 2:
				value = "所属行业";
				break;
			case 3:
				value = "投资金额(万元)";
				break;
			case 4:
				value = "PPP实施阶段";
				break;
			case 5:
				value = "拟合作期限(年)";
				break;
			}
			cell.setCellValue(value);
		}

	}

	/**
	 * 判断Excel的版本,获取Workbook
	 * 
	 * @param in
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static Workbook getWorkbok(File file) throws IOException {
		Workbook wb = null;
		if (file.getName().endsWith(EXCEL_XLS)) { // Excel 2003
			wb = new HSSFWorkbook();
		} else if (file.getName().endsWith(EXCEL_XLSX)) { // Excel 2007/2010
			wb = new XSSFWorkbook();
		}
		return wb;
	}
}