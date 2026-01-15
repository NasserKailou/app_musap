package modules;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.jooq.Configuration;
import com.google.inject.Inject;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.ExporterInput;
import net.sf.jasperreports.export.OutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;

/**
 * 
 * @author nasser
 *
 */
public class CallJasperReport {

	private String reportsDir = new File("").getAbsolutePath() + "/reports/spool/"; // env.rootPath().getAbsolutePath()
	private String templateDir = new File("").getAbsolutePath() + "/reports/templates/"; // env.rootPath().getAbsolutePath()
	private final SimpleDateFormat date = new SimpleDateFormat("MM-dd-yyyy_HH:mm:ss");

	private final IConnectionHelper con;
	private static Configuration conf;

	@Inject
	public CallJasperReport(IConnectionHelper con, Configuration conf) {
		super();
		this.con = con;
		this.conf = this.con.connection().configuration();
	}

	Connection conn = conf.connectionProvider().acquire();

	public void generateReport(String fileName, String id) throws IOException {
		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));

		try {
			String compileFile = templateDir + fileName + ".jasper";

			// First, compile jrxml file.
			// JasperReport jasperReport =
			// JasperCompileManager.compileReport(reportSrcFile);

			// Connection conn = conf.connectionProvider().acquire();
			System.out.println("connexion :" + conn);
			System.out.println("impression en cours...." + id);
			// Parameters for report
			Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put("matricule", id);

			JasperPrint print = JasperFillManager.fillReport(compileFile, parameters, conn);
			System.out.println("impression en cours....###");
			// Parameters for report
			// Make sure the output directory exists.
			File outDir = new File(reportsDir);
			outDir.mkdirs();

			// PDF Exportor.
			JRPdfExporter exporter = new JRPdfExporter();

			ExporterInput exporterInput = new SimpleExporterInput(print);
			// ExporterInput
			exporter.setExporterInput(exporterInput);

			// ExporterOutput
			OutputStreamExporterOutput exporterOutput = new SimpleOutputStreamExporterOutput(
					reportsDir + fileName + "_" + now_string + "_" + id + ".pdf");
			// Output
			exporter.setExporterOutput(exporterOutput);

			//
			SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
			exporter.setConfiguration(configuration);
			exporter.exportReport();

			System.out.print("Done!");

		} catch (JRException e) {
			System.out.println(e.getMessage());
		}

	}

	public void generateReport(String fileName, Timestamp date1, Timestamp date2, String partID) throws IOException {
		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

		try {
			String compileFile = templateDir + fileName + ".jasper";

			// First, compile jrxml file.
			// JasperReport jasperReport =
			// JasperCompileManager.compileReport(reportSrcFile);

			// Connection conn = conf.connectionProvider().acquire();
			System.out.println("impression en cours...." + date1);
			// Parameters for report
			Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put("date1", date1);
			parameters.put("date2", date2);
			parameters.put("matricule", partID);
			JasperPrint print = JasperFillManager.fillReport(compileFile, parameters, conn);
			System.out.println("impression en cours....###");
			// Parameters for report
			// Make sure the output directory exists.
			File outDir = new File(reportsDir);
			outDir.mkdirs();

			// PDF Exportor.
			JRPdfExporter exporter = new JRPdfExporter();

			ExporterInput exporterInput = new SimpleExporterInput(print);
			// ExporterInput
			exporter.setExporterInput(exporterInput);

			// ExporterOutput
			OutputStreamExporterOutput exporterOutput = new SimpleOutputStreamExporterOutput(
					reportsDir + fileName + "_" + now_string + "_" + "" + ".pdf");
			// Output
			exporter.setExporterOutput(exporterOutput);

			//
			SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
			exporter.setConfiguration(configuration);
			exporter.exportReport();

			System.out.print("Done!");

		} catch (JRException e) {
			System.out.println(e.getMessage());
		}

	}

	public void generateReport(String fileName, Timestamp date1, Timestamp date2) throws IOException {
		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

		try {
			String compileFile = templateDir + fileName + ".jasper";

			// get connexion
			// Connection conn = conf.connectionProvider().acquire();

			System.out.println("impression en cours...." + date1);
			// Parameters for report
			Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put("date1", date1);
			parameters.put("date2", date2);
			JasperPrint print = JasperFillManager.fillReport(compileFile, parameters, conn);
			System.out.println("impression en cours....###");
			// Parameters for report
			// Make sure the output directory exists.
			File outDir = new File(reportsDir);
			outDir.mkdirs();

			// PDF Exportor.
			JRPdfExporter exporter = new JRPdfExporter();

			ExporterInput exporterInput = new SimpleExporterInput(print);
			// ExporterInput
			exporter.setExporterInput(exporterInput);

			// ExporterOutput
			OutputStreamExporterOutput exporterOutput = new SimpleOutputStreamExporterOutput(
					reportsDir + fileName + "_" + now_string + "_" + "" + ".pdf");
			// Output
			exporter.setExporterOutput(exporterOutput);

			//
			SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
			exporter.setConfiguration(configuration);
			exporter.exportReport();

			System.out.print("Done!");

		} catch (JRException e) {
			System.out.println(e.getMessage());
		}

	}

	public void generateReport(String fileName, Timestamp date1) throws IOException {
		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

		try {
			String compileFile = templateDir + fileName + ".jasper";

			// First, compile jrxml file.
			// JasperReport jasperReport =
			// JasperCompileManager.compileReport(reportSrcFile);

			// Connection conn = conf.connectionProvider().acquire();
			System.out.println("impression en cours...." + date1);
			// Parameters for report
			Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put("id", date1);

			JasperPrint print = JasperFillManager.fillReport(compileFile, parameters, conn);
			System.out.println("impression en cours....###");
			// Parameters for report
			// Make sure the output directory exists.
			File outDir = new File(reportsDir);
			outDir.mkdirs();

			// PDF Exportor.
			JRPdfExporter exporter = new JRPdfExporter();

			ExporterInput exporterInput = new SimpleExporterInput(print);
			// ExporterInput
			exporter.setExporterInput(exporterInput);

			// ExporterOutput
			OutputStreamExporterOutput exporterOutput = new SimpleOutputStreamExporterOutput(
					reportsDir + fileName + "_" + now_string + "_" + "" + ".pdf");
			// Output
			exporter.setExporterOutput(exporterOutput);

			//
			SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
			exporter.setConfiguration(configuration);
			exporter.exportReport();

			System.out.print("Done!");

		} catch (JRException e) {
			System.out.println(e.getMessage());
		}

	}
}
