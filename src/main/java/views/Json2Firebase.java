package views;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;

public class Json2Firebase {
    public JButton optionsButton;
    public JTextField fileName;
    public JButton generateButton;
    public RSyntaxTextArea editor;
    public JPanel rootView;
    public OnGenerateClicked listener;

    public void setOnGenerateListener(OnGenerateClicked listener) {
        this.listener = listener;
        generateButton.addActionListener(action -> {
            if (this.listener != null) {
                this.listener.onClicked(
                        fileName != null ? fileName.getText() : "response",
                        editor != null ? editor.getText() : ""
                );
            }
        });
    }

    public interface OnGenerateClicked {
        void onClicked(String fileName, String json);
    }
}
