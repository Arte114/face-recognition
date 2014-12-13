import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class FaceCheckSrv extends HttpServlet{

private ServletConfig config=null;

	public void init(ServletConfig cf)throws ServletException
	{
		config=cf;
		System.out.println("------------------------------");
		System.out.println("Initializing the ImageRecvSrv");
		System.out.println("------------------------------");
//Taking the path of the existing images from web.xml
//
	}
	public void doPost(HttpServletRequest req,
					HttpServletResponse res)throws ServletException,IOException
	{

	System.out.println("------------------------------");
	System.out.println("Checking the Uploaded Image...");
	System.out.println("------------------------------");
	ServletContext ctx=config.getServletContext();
    try 
	{
        EigenFaceCreator creator = new EigenFaceCreator();
		String dir=ctx.getInitParameter("imagedir");
		System.out.println("The Image Directory is : "+dir);
		String file=(String)ctx.getAttribute("userimage");
		System.out.println("The Image File to be checked : "+file);
		PrintWriter out=res.getWriter();

        //creator.USE_CACHE = -1;
        System.out.println("Constructing face-spaces from "+dir+" ...");
        creator.readFaceBundles(dir);

        System.out.println("Comparing "+file+" ...");
        String result = creator.checkAgainst(file);
		if(creator.DISTANCE==0)
		{
			out.println("Matching");
		}
		else
		{
			out.println("NOt Matching");
		}
        System.out.println("Most closly reseambling: "+result+" with "+creator.DISTANCE+" distance.");
      }
	  catch (Exception e) 
	  {
		  e.printStackTrace(); 
	  }
    }
}