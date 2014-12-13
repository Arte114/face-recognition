import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class ImageRecvSrv extends HttpServlet{

private ServletConfig config=null;

	public void init(ServletConfig cf)throws ServletException
		{
		config=cf;
		System.out.println("------------------------------");
		System.out.println("Initializing the ImageRecvSrv");
		System.out.println("------------------------------");
		}

	public void doGet(HttpServletRequest req,
					HttpServletResponse res)throws ServletException,IOException
	{
		System.out.println("This is the doGet()");
		System.out.println("Caling the doPost()");
			doPost(req,res);
	}

	public void doPost(HttpServletRequest req,
					HttpServletResponse res)throws ServletException,IOException
	{
	System.out.println("This is the doPost()");
	PrintWriter out=res.getWriter();
	res.setContentType("text/html");
	System.out.println("------------------------------");
	System.out.println("Recieving the Uploaded Image...");


	ServletContext ctx=config.getServletContext();	
   
//	Object userimage=null;
	System.out.println("------------------------------");
	
	ctx.setAttribute("userimage","c:\\frs\\MyImage.jpg");
	res.sendRedirect("http://localhost:7001/FRSNEW/facecheck");
/*	RequestDispatcher rd=ctx.getRequestDispatcher("/facecheck");
	rd.forward(req,res);
*/
//	System.out.println("Image placed inside the ServletContext Object");

	}

}
