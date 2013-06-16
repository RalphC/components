package org.apache.jmeter.visualizers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jmeter.samplers.SampleResult;

import com.unionpay.upmp.sdk.util.UpmpCore;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

public class RenderAsUPMP extends SamplerResultTab implements ResultRenderer {

    private static final String ESC_CHAR_REGEX = "\\\\[\"\\\\/bfnrt]|\\\\u[0-9A-Fa-f]{4}"; // $NON-NLS-1$

    private static final String NORMAL_CHARACTER_REGEX = "[^\"\\\\]";  // $NON-NLS-1$

    private static final String STRING_REGEX = "\"(" + ESC_CHAR_REGEX + "|" + NORMAL_CHARACTER_REGEX + ")*\""; // $NON-NLS-1$

    // This 'other value' regex is deliberately weak, even accepting an empty string, to be useful when reporting malformed data.
    private static final String OTHER_VALUE_REGEX = "[^\\{\\[\\]\\}\\,]*"; // $NON-NLS-1$

    private static final String VALUE_OR_PAIR_REGEX = "((" + STRING_REGEX + "\\s*:)?\\s*(" + STRING_REGEX + "|" + OTHER_VALUE_REGEX + ")\\s*,?\\s*)"; // $NON-NLS-1$

    private static final Pattern VALUE_OR_PAIR_PATTERN = Pattern.compile(VALUE_OR_PAIR_REGEX);
	
	@Override
	public void renderResult(SampleResult sampleResult) {
		showRenderUPMPResponse(sampleResult);
	}
	
	private String convertUPMPResponse(String response){
		String converted = "";
		Map<String, String> para = null;
		try {
			para = UpmpCore.parseQString(response);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ObjectMapper mapper = new ObjectMapper();
		try {
			converted = mapper.writeValueAsString(para);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return converted;
	}
	
    private void showRenderUPMPResponse(SampleResult sampleResult) {
		String response = ViewResultsFullVisualizer.getResponseAsString(sampleResult);
		if (response != null) {
			response = convertUPMPResponse(response);
		}
        results.setContentType("text/plain"); // $NON-NLS-1$
        results.setText(response == null ? "" : prettyJSON(response));
        results.setCaretPosition(0);
        resultsScrollPane.setViewportView(results);
    }
    
    private static String prettyJSON(String json) {
        StringBuilder pretty = new StringBuilder(json.length() * 2); // Educated guess

        final String tab = ":   "; // $NON-NLS-1$
        StringBuilder index = new StringBuilder();
        String nl = ""; // $NON-NLS-1$

        Matcher valueOrPair = VALUE_OR_PAIR_PATTERN.matcher(json);

        boolean misparse = false;

        for (int i = 0; i < json.length(); ) {
            final char currentChar = json.charAt(i);
            if ((currentChar == '{') || (currentChar == '[')) {
                pretty.append(nl).append(index).append(currentChar);
                i++;
                index.append(tab);
                misparse = false;
            }
            else if ((currentChar == '}') || (currentChar == ']')) {
                if (index.length() > 0) {
                    index.delete(0, tab.length());
                }
                pretty.append(nl).append(index).append(currentChar);
                i++;
                int j = i;
                while ((j < json.length()) && Character.isWhitespace(json.charAt(j))) {
                    j++;
                }
                if ((j < json.length()) && (json.charAt(j) == ',')) {
                    pretty.append(","); // $NON-NLS-1$
                    i=j+1;
                }
                misparse = false;
            }
            else if (valueOrPair.find(i) && valueOrPair.group().length() > 0) {
                pretty.append(nl).append(index).append(valueOrPair.group());
                i=valueOrPair.end();
                misparse = false;
            }
            else {
                if (!misparse) {
                    pretty.append(nl).append("- Parse failed from:");
                }
                pretty.append(currentChar);
                i++;
                misparse = true;
            }
            nl = "\n"; // $NON-NLS-1$
        }
        return pretty.toString();
    }
    
    @Override
    public String toString() {
        return "JSON@UPMP"; // $NON-NLS-1$
    }

}
