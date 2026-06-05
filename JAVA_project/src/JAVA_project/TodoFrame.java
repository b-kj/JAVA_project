package JAVA_project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class TodoFrame extends JFrame {
    private int roomnum;
    private DB db;
    private DefaultListModel<TodoDTO> listModel;
    private JList<TodoDTO> todoList;
    private JTextField contentField;
    private JProgressBar progressBar;

    public TodoFrame(int roomnum, DB db) {
        this.roomnum = roomnum;
        this.db = db;

        setTitle("투두 리스트 (Room: " + roomnum + ")");
        setSize(350, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 상단 패널 (진행률 바 + 입력창)
        JPanel topPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(50, 205, 50)); // 초록색 계열 포인트
        
        JPanel inputPanel = new JPanel(new BorderLayout());
        contentField = new JTextField();
        JButton addButton = new JButton("추가");
        inputPanel.add(contentField, BorderLayout.CENTER);
        inputPanel.add(addButton, BorderLayout.EAST);
        
        topPanel.add(progressBar, BorderLayout.NORTH);
        topPanel.add(inputPanel, BorderLayout.SOUTH);

        // 목록 패널
        listModel = new DefaultListModel<>();
        todoList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(todoList);

        // 하단 설명 레이블
        JLabel infoLabel = new JLabel("항목을 더블클릭하면 상태(진행중/완료)가 변경됩니다.", SwingConstants.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(infoLabel, BorderLayout.SOUTH);

        loadTodos();

        // 추가 이벤트
        addButton.addActionListener(e -> {
            String content = contentField.getText().trim();
            if (!content.isEmpty()) {
                db.addTodo(roomnum, content);
                loadTodos();
                contentField.setText("");
            }
        });

        // 리스트 더블클릭 이벤트 (완료 처리)
        todoList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) { 
                    int index = todoList.locationToIndex(evt.getPoint());
                    if (index >= 0) {
                        TodoDTO selected = listModel.getElementAt(index);
                        db.updateTodoStatus(selected.getNum(), !selected.isCompleted());
                        loadTodos(); // 갱신하며 진행률 바도 함께 업데이트
                    }
                }
            }
        });

        setVisible(true);
    }

    private void loadTodos() {
        listModel.clear();
        ArrayList<TodoDTO> todos = db.getTodos(roomnum);
        int total = todos.size();
        int completedCount = 0;

        for (TodoDTO t : todos) {
            listModel.addElement(t);
            if (t.isCompleted()) {
                completedCount++;
            }
        }

        // 진행률 업데이트
        if (total == 0) {
            progressBar.setValue(0);
            progressBar.setString("진행률 : 0%");
        } else {
            int progress = (int) Math.round((double) completedCount / total * 100);
            progressBar.setValue(progress);
            progressBar.setString("진행률 : " + progress + "%");
        }
    }
}
