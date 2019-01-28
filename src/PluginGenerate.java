import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by admin on 2019/1/24.
 */
public class PluginGenerate extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Project project = e.getData(PlatformDataKeys.PROJECT);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        Document document = editor.getDocument();
            int length = document.getTextLength();

            String codeString = document.getText();
            List<String> records;
            records = findRecord(codeString);
            if (records.isEmpty()) {
                Messages.showMessageDialog(
                        "No Record Declaration Found!\nFormat:'-record(peopel, {name=\"\",age=21}).'\nEnter to the code.",
                        "Not Find!",
                        null);
            } else {

                String code = "";
                for (String record : records) {
                    code += record2code(record);
                }
                final String finalCode = code;
                WriteCommandAction.runWriteCommandAction(project, () ->
                        document.insertString(length, finalCode)
                );
            }
    }


    //在文档中找出record标签
    private static List<String> findRecord(String text) {
        text = text.replaceAll("%.*", "");
        String pattern = "(-record\\([\\w\\s,\\{=\\\"\\[\\]\\}]*\\).)";
        Pattern p = Pattern.compile(pattern);
        Matcher matcher = p.matcher(text);
        List<String> record = new ArrayList<>();
        while (matcher.find()) {
            record.add(matcher.group(0));
            text = matcher.replaceFirst("");
            matcher = p.matcher(text);
        }
        return record;
    }

    //将record标签内容变为对应的代码
    private static String record2code(String text) {
        text = text.replaceAll("\\s*", "");
        String pattern = "(?<=[(\\{\\,])(\\w+)(?=[\\,\\=])";
        Pattern p = Pattern.compile(pattern);
        System.out.println("Text:" + text);
        Matcher matcher = p.matcher(text);
        List<String> records = new ArrayList<>();
        while (matcher.find()) {
            records.add(matcher.group(1));
            text = matcher.replaceFirst("");
            matcher = p.matcher(text);
        }
        String entity = records.get(0);
        String note = "\n\n\n%% -----------------------------------------------------------------\n" +
                "%% Description: It's " + entity + " related getter setter method.\n" +
                "%% -----------------------------------------------------------------";
        String initCode = note + "\ninit_" + entity + "()->\n\t#" + entity + "{}.\n";
        String getSetCode = "";
        for (int i = 1; i < records.size(); i++) {
            String attr = records.get(i);
            getSetCode += "\nget_" + entity + "_" + attr + "(D)->\n\tD#" + entity + "." + attr + ".\n" +
                    "\nset_" + entity + "_" + attr + "(D, V)->\n\tD#" + entity + "{" + attr + " = V}.\n";
        }
        return initCode + getSetCode;
    }
}
