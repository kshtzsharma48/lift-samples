package bootstrap.liftweb

import net.liftweb.util._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import Helpers._
import net.liftweb.mapper.{DB, ConnectionManager, Schemifier, DefaultConnectionIdentifier, ConnectionIdentifier}
import java.sql.{Connection, DriverManager}
import com.liftcode.unconference._
import model._
import snippet._

/**
* A class that's instantiated early and run.  It allows the application
* to modify lift's environment
*/
class Boot {
  def boot {
    DefaultConnectionIdentifier.jndiName = "unconf"
    
    if (!DB.jndiJdbcConnAvailable_?) DB.defineConnectionManager(DefaultConnectionIdentifier, DBVendor)
    // where to search snippet
    LiftRules.addToPackages("com.liftcode.unconference")     
    Schemifier.schemify(true, Log.infoF _, User, Entry, Feedback)
    
    val areas = Entry.areas
    
    S.addAround(User.requestLoans)
    
    LiftRules.addDispatchBefore {
      case RequestMatcher(_, ParsePath("login" :: Nil, _, _), _, _) => Login.login
      case RequestMatcher(_, ParsePath("logout" :: Nil, _, _), _, _) => Login.logout
      case RequestMatcher(r, _, _, _) if r.uri.endsWith("/home/index") => 
      ignore => Full(RedirectResponse(r.updateWithContextPath("/")))
      
      case RequestMatcher(r, ParsePath("redirect_to" :: "edit" :: which :: page :: _, _, _), _, _) =>
      ignore => Full(RedirectResponse("/"+which+"/edit/"+page))
      
      case RequestMatcher(r, ParsePath("go" :: "home" :: _, _, _), _, _) =>
      ignore => Full(RedirectResponse("/"))
    }
    
    LiftRules.addRewriteBefore {      
      case RewriteRequest(ParsePath(which :: "add" :: Nil, _,_), _, httpReq) 
      if (which == Entry.News || which == Entry.Sessions) && httpReq.getParameter("name") != null &&
      httpReq.getParameter("name").length > 3 =>
      RewriteResponse( List("redirect_to", "edit", which, urlEncode(httpReq.getParameter("name"))))
      
      case RewriteRequest(ParsePath(which :: "add" :: _, _,_), _, httpReq) 
      =>
      RewriteResponse( List("go", "home"))

      case RewriteRequest(ParsePath("index" :: Nil, _,_), _, _) =>
      RewriteResponse( List("the_wiki", "view", "home", "index"))
      
      case RewriteRequest(ParsePath(which :: Nil, _,_), _, _) 
      if areas.contains(which) =>
      RewriteResponse( List("the_wiki", "main"), Map("category" -> which))
      
      case RewriteRequest(ParsePath("edit" :: Nil, _,_), _, _) |
      RewriteRequest(ParsePath("home" :: "edit" :: Nil, _,_), _, _)      
      =>
      RewriteResponse( List("the_wiki", "edit", "home", "index"))
      
      case RewriteRequest(ParsePath(which :: what :: Nil, _,_), _, _) 
      if areas.contains(which) =>
      RewriteResponse( List("the_wiki", "view", which, what))
      
      case RewriteRequest(ParsePath(which :: "edit" :: what :: Nil, _,_), _, _) 
      if areas.contains(which) =>
      RewriteResponse( List("the_wiki", "edit", which, what))
      
            case RewriteRequest(ParsePath(which :: "history" :: what :: Nil, _,_), _, _) 
      if areas.contains(which) =>
      RewriteResponse( List("the_wiki", "history", which, what))
      
      case RewriteRequest(ParsePath("the_wiki" :: cmd :: category :: page :: Nil, _,_), _, _)
      if (cmd == "view" || cmd == "edit" || cmd == "history") && areas.contains(category) =>
      RewriteResponse(List("the_wiki", cmd), Map("category" -> category, "page" -> urlDecode(page)) )
      
    }
  }
}

object DBVendor extends ConnectionManager {
  def newConnection(name: ConnectionIdentifier): Can[Connection] = {
    try {
      Class.forName("org.postgresql.Driver")
      val dm = DriverManager.getConnection("jdbc:postgresql://localhost/unconf_dev", "dpp", "")
      Full(dm)
    } catch {
      case e : Exception => e.printStackTrace; Empty
    }
  }
  def releaseConnection(conn: Connection) {conn.close}
}

