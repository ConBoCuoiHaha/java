import com.eduactivity.security.IdentityV3PasswordHasher;
public class HashGen{
  public static void main(String[] args){
    for(String p: args){
      System.out.println(p+" => "+IdentityV3PasswordHasher.hash(p));
    }
  }
}
