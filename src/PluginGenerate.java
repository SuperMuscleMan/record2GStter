import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PluginGenerate extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    // TODO: insert action logic here
    Project project = e.getData(PlatformDataKeys.PROJECT);
    Editor editor = e.getData(PlatformDataKeys.EDITOR);
    Document document = editor.getDocument();
    GenerateGSetter(project, document);
  }

  private void GenerateGSetter(Project project, Document document) {
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
      String export = "\n";
      for (String record : records) {
        code += record2code(record).get(0);
        export += "-export([" + record2code(record).get(1) + "]).\n";
      }
      int exportIndex = findExport(codeString);
      final String finalCode = code;
      final String exportCode = export;
//          写入代码
      WriteCommandAction.runWriteCommandAction(project, () -> {
            document.insertString(exportIndex, exportCode);
            document.insertString(document.getTextLength(), finalCode);
          }
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
  private static Map record2code(String text) {
    text = text.replaceAll("\\s*", "");
    String pattern = "(?<=[(\\{\\,])(\\w+)(?=[\\,\\=])";
    Pattern p = Pattern.compile(pattern);
    Matcher matcher = p.matcher(text);
    List<String> records = new ArrayList<>();
    while (matcher.find()) {
      records.add(matcher.group(1));
      text = matcher.replaceFirst("");
      matcher = p.matcher(text);
    }
    String entity = records.get(0);
    String note = "\n\n\n%% -----------------------------------------------------------------\n" +
        "%% --- Description: It's " + entity + " related getter setter method.\n" +
        "%% -----------------------------------------------------------------";
    String initCode = note + "\ninit_" + entity + "()->\n\t#" + entity + "{}.\n";
    String getSetCode = "";
    String exportCode = "init_" + entity + "/0";
    for (int i = 1; i < records.size(); i++) {
      String attr = records.get(i);
      getSetCode += gsetter(entity, attr);
      exportCode += export(entity, attr);
    }
    Map<Integer, String> code = new HashMap<>();
    code.put(0, initCode + getSetCode);
    code.put(1, exportCode);
    return code;
  }

  private static String gsetter(String entity, String attr) {
    return "\nget_" + entity + "_" + attr + "(D)->\n\tD#" + entity + "." + attr + ".\n" +
        "\nset_" + entity + "_" + attr + "(D, V)->\n\tD#" + entity + "{" + attr + " = V}.\n";
  }

  private static String export(String entity, String attr) {
    return ",get_" + entity + "_" + attr + "/1" + ",set_" + entity + "_" + attr + "/2";
  }

  //    查找-exprot([结束的索引点
  private static int findExport(String text) {
    int index = text.indexOf("-export([");
    if (index == -1) {
      index = text.indexOf("-record");
    }
    return index;
  }
}
