import java.io.File;
import java.util.List;
import com.validador.aces.parsers.ExcelApplicationParser;

public class DebugSheets {
    public static void main(String[] args) throws Exception {
        File file = new File("ACES/ACES Keep on Green 23.09.2026 - Copy.xlsx");
        List<String> sheets = ExcelApplicationParser.readSheetNames(file);
        System.out.println("Hojas disponibles:");
        for (String s : sheets) {
            System.out.println("  - " + s);
        }
    }
}
