import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import com.oreilly.servlet.*;
import com.oreilly.servlet.multipart.*;

public class DemoParserUploadServlet extends HttpServlet 
{
	private File dir;
	String dr;
	EigenFaceCreator creator;
	public void init(ServletConfig config) throws ServletException 
	{
		super.init(config);
    // Read the uploadDir from the servlet parameters
		String dirName = config.getInitParameter("uploadDir");
		if(dirName == null) 
		{
			throw new ServletException("Please supply uploadDir parameter");
		}
		dir = new File(dirName);
		if(! dir.isDirectory()) 
		{ 
			throw new ServletException("Supplied uploadDir " + dirName +" is invalid");
		}
		dr=config.getInitParameter("imagedir");
		if(dr == null) 
		{
			throw new ServletException("Please supply Image Bundle Directory parameter");
		}
		creator = new EigenFaceCreator();
		System.out.println("<br>The Image Directory is : "+dr);
  }//init
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
  {
	 PrintWriter out = response.getWriter();
   	 response.setContentType("text/plain");
	 out.println("<html><body bgcolor='khaki' text='green'>");
         String fileName="";
       try 
	{
		MultipartParser mp = new MultipartParser(request, 10*1024*1024); // 10MB
		Part part;
		while ((part = mp.readNextPart()) != null) 
		{
			String name = part.getName();
			if (part.isParam()) 
			{
          // it's a parameter part
				ParamPart paramPart = (ParamPart) part;
				String value = paramPart.getStringValue();
			}
			else if (part.isFile()) 
			{
          // it's a file part
				FilePart filePart = (FilePart) part;
				fileName = filePart.getFileName();
				if (fileName != null) 
				{
            // the part actually contained a file
					long size = filePart.writeTo(dir);
					
					out.println("<br><b>File Name::</b>" + name + "<br><b>FilePath::</b>" + filePart.getFilePath());
					out.println("<br><b>Content Type::</b>" + filePart.getContentType() + "<br><b>Size::</b>" + size);
				}
				else 
				{ 
		            // the field did not contain a file
				    out.println("<br>File-name=" + name + " is EMPTY");
		        }
				out.flush();
			}
		}//while
		System.out.println("------------------------------");
		System.out.println("------------------------------");
		System.out.println("CHECKING THE UPLOADED IMAGE...");
		System.out.println("------------------------------");
		System.out.println("------------------------------");
		try 
		{
			String file=fileName;
			System.out.println("The Image File To Be Checked : "+file);
			//creator.USE_CACHE = -1;
			System.out.println("Constructing Face-Spaces From "+dr+" ...");
			creator.readFaceBundles(dr);
			System.out.println("COMPARING "+file+" ...");
			String result = creator.checkAgainst(file);
			if(creator.DISTANCE<=0.0)
			{
				out.println("<br><b>A Match is Found For Uploaded Image..");
				out.println("<br><b><CENTER><h2>AUTHENTICATION SUCCESS</CENTER></h2>");
				
				//out.println("</B><BR><INPUT TYPE='image' SRC="+file+" name='img'>");
			}
			else
			{
				out.println("<br><br><br><B>No Match Found For Uploaded Image In our Database.");
				out.println("<br><b><center><h2>SORRY, AUTHENTICATION FAILED.</center></b></h2>");
				//out.println("<br><center> Try Again.. <a href='upload.html'> HOME </a></b></center>");
			}
			System.out.println("Most Closly Reseambling: "+result+" WITH  "+creator.DISTANCE+" Of Distance.");
      }
	  catch (Exception e) 
	  {
		  e.printStackTrace(); 
	  }
    } 
    catch (IOException lEx) 
	{
      System.out.println("Log Error Exception..");
    }
  }
}
