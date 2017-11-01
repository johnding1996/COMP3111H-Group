package src.main.java.agent;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import controller.ParserMessageJSON;
import controller.Publisher;
import controller.FormatterMessageJSON;

public class PraseMeauFromWeb {
 
    /**
     * returns a JSON array from given URL, corresponding to "meau" on QueryJSON
     * @param userId String of user Id
     * with reference from http://carlofontanos.com/java-parsing-json-data-from-a-url/
     */
	public JSONArray webGet(String webLink) {
		JSONParser parser = new JSONParser();
		JSONArray output = new JSONArray();
     
		try {         
			URL oracle = new URL(webLink); // URL to Parse
			URLConnection yc = oracle.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));            
                     
			String inputLine;
			while ((inputLine = in.readLine()) != null) {               
				JSONArray a = (JSONArray) parser.parse(inputLine);
             
				// Loop through each item
				for (Object o : a) {
					JSONObject tutorials = (JSONObject) o;

					String name = (String) tutorials.get("name");
					double price = (double) tutorials.get("price");
					JSONArray ingredients = (JSONArray) tutorials.get("ingredients");
					JSONObject newFood = new JSONObject();
					newFood.put("name", name);
					newFood.put("portionSize", 0.0);
					newFood.put("portionUnit", "");
					newFood.put("ingredients", ingredients);
					newFood.put("price", price);
					output.add(newFood);
                 
				}
			}
			in.close();
        
		//if exists error in reading
		} catch (Exception e) {
			log.info("Invalid URL");
			return null;
		} 

// Use for debug
//        catch (IOException e) {
//          e.printStackTrace();
//      } catch (ParseException e) {
//          e.printStackTrace();
//      }
     
		return output;   
	}   
}
