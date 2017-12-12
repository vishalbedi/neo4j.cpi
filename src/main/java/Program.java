import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Program {
    public static void main(String[] args) {
        int[] validInputs = new int[]{8, 16, 32, 64, 128, 256};
        System.out.println("Enter node count to Sub graph. Valid Values are 8, 16, 32, 64, 128, 256");
        try{
            Scanner scanIn = new Scanner(System.in);
            int choice = scanIn.nextInt();
            if(!Arrays.stream(validInputs).anyMatch(i -> i == choice)){
                System.out.println("Not a valid Input : " + choice);
                return;
            }
            Application application = new Application();
            application.start(choice);
        }
        catch (InputMismatchException ex){
            System.out.println("Try again. The input you provided was not correct");
        }
    }
}
