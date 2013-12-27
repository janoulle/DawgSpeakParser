import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Iterator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/*
 * Author: Jane Ullah
 * Project: Scrapes http://www.english.uga.edu/def/ and stores the Slanguage dictionary
 * into a file or database
 * Dependencies: jsoup-1.7.2.jar
 */
public class DawgSpeakParser {
	/**
	 * Use this USERAGENT to avoid getting band if you use scrape directly from a URL
	 */
	public static final String USERAGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:16.0) Gecko/20100101 Firefox/16.0";
	/**
	 * Replace YOURDATABASENAME with whatever database you're using
	 */
	static String JDBC_URL = "jdbc:mysql://localhost/data?autoReconnect=true";
	/**
	 * Set the value of WORDSELECTOR to the selector expression that will get the data you're interested in. Remember to experiment with the console in Chrome.
	 */
	public static final String WORDSELECTOR = "div.row>div";
	public static final String SELECTOR = "div.row";
	/**
	 * Use this PreparedStatement for when you want to insert items into
	 * the database
	 */
	private static PreparedStatement insertStmt;
	/**
	 * Connection object for database actions
	 */
	private final static String URL = "http://www.english.uga.edu/def/";
	private static Connection conn = null;
	private final static String DAWGSPEAK = "DawgSpeak";
	private final static String USERNAME = "demo";
	private final static String PASSWORD = "demo";

	/**
	 * Constructor to open up a connection to the database
	 */
	public DawgSpeakParser(){
		try {
			//load the driver
			Class.forName("com.mysql.jdbc.Driver");
			//set the username and password for accessing the database
			conn = DriverManager.getConnection(JDBC_URL, USERNAME,PASSWORD);
			//Prep the preparedstatement for insertion
			insertStmt = conn.prepareStatement("insert into " + DAWGSPEAK + " (word,type,definition) values(?,?,?)");
		} catch (Exception e) {
			System.out.println("Error in main: " + e.getClass().getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Main method
	 * @param args
	 */
	public static void main(String[] args) {
		try{
			//Create a Scraper object once you have created your database and tables proper
			DawgSpeakParser obj = new DawgSpeakParser();
			/*File input = new File("data.html");
			//Check for the presence of the data file
			if (input.exists() && input.canRead()){
				Document doc = Jsoup.parse(input,"UTF-8");
				//Apply selector
				Elements results = doc.select(WORDSELECTOR);
				writeResults(results,"results.txt");
			} else{
				System.out.println("Please make sure " + input.getName() + " exists and has the appropriate read permissions.");
			}*/
			Document doc = Jsoup.connect(URL).userAgent(USERAGENT).get();
			Elements results = doc.select(WORDSELECTOR);
			writeResults(results,"results.txt");
		} catch (Exception e){
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Parse data from the Elements object to
	 * 1. write to the database (word, type and definition)
	 * 2. write the data to a text file
	 * @param links (Elements object)
	 * @param file (File object)
	 * @param obj (Scraper object)
	 * @throws IOException
	 * @throws Exception
	 */
	public static void writeResults(Elements links, String file) throws IOException, Exception {
		String word = "", type = "", definition = "", line = "";
		int count = 1;
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		BufferedWriter writeCSV = new BufferedWriter(new FileWriter("words.csv"));
		writeCSV.append("word,type,definition");
		writeCSV.append('\n');

		for (Element e:links){
			//Write the string to the destination specified by file
			line = e.text();
			writer.write(line);
			writer.newLine();
			/*Code to parse the line and break up into the following parts:
			word, type and definition. E.g. this line:
			"12 (noun) If someone says 12, they are referring to the police. This is usually shouted as a warning when the police arrive somewhere unexpectedly. I was at the party and someone shouted "12" and everyone scattered."
			becomes:
			* word: 12
			* type: noun
			* definition: If someone says 12, they are referring to the police. This is usually shouted as a warning when the police arrive somewhere unexpectedly. I was at the party and someone shouted "12" and everyone scattered.
			* Uncomment the switch statement when you've created your database and tables
			* as the readme instructs.
			*/
			switch(count){
				//1 --> word, 2 --> definition
				case(1):
					word = line;
					count++;
					break;
				case(2):
					int lparen = line.indexOf("(");
					int rparen = line.indexOf(")");

					//All the definitions should have the word type in brackets but
					//obviously,there might be instances where this is not the case so
					//definitions would be missing. No bueno.
					if (lparen >= 0 && rparen > 0){
						type = line.substring(lparen+1,rparen);
						definition = line.substring(rparen+1).replaceAll("“", "\"").replaceAll("”","\"").replaceAll("‘","'").replaceAll("’","'").replaceAll("—", "-");

						writeCSV.append(word);
						writeCSV.append(',');
						writeCSV.append(type);
						writeCSV.append(',');
						writeCSV.append('\n');
						writeCSV.append(definition);
						insertStmt.setString(1, word);
						insertStmt.setString(2, type);
						insertStmt.setString(3,  definition);
						insertStmt.executeUpdate();
						count = 1;
						continue;
					}
					count++;
					break;
				default:
					continue;
			}
		}
		writer.close();
		writeCSV.close();
	}
}

