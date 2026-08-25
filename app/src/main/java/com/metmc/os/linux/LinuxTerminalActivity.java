package com.metmc.os.linux;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.*;

import java.io.*;

public class LinuxTerminalActivity extends Activity {

    private TextView output;
    private EditText command;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(8,8,10));
        root.setPadding(12,12,12,12);

        TextView title = new TextView(this);
        title.setText("METMC Linux Terminal");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);

        root.addView(title,
            new LinearLayout.LayoutParams(-1,60));

        ScrollView scroll = new ScrollView(this);

        output = new TextView(this);
        output.setTextColor(Color.WHITE);
        output.setTypeface(Typeface.MONOSPACE);
        output.setTextSize(13);
        output.setText("root@metmc:~$ Connected\n\n");

        scroll.addView(output);

        root.addView(scroll,
            new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout row = new LinearLayout(this);

        command = new EditText(this);
        command.setSingleLine(true);
        command.setTextColor(Color.WHITE);
        command.setHintTextColor(Color.GRAY);
        command.setHint("Linux command");
        command.setTypeface(Typeface.MONOSPACE);

        Button run = new Button(this);
        run.setText("Run");

        row.addView(command,
            new LinearLayout.LayoutParams(0,60,1));

        row.addView(run,
            new LinearLayout.LayoutParams(150,60));

        root.addView(row);

        setContentView(root);

        run.setOnClickListener(v -> execute());
        command.setOnEditorActionListener((v,a,e) -> {
            execute();
            return true;
        });
    }

    private void execute() {
        String cmd = command.getText().toString().trim();

        if (cmd.isEmpty())
            return;

        output.append(
            "\nroot@metmc:~$ " + cmd + "\n"
        );

        command.setText("");

        new Thread(() -> {
            String result;

            try {
                Process p = new ProcessBuilder(
                    "su", "-c",
                    "chroot /data/local/linux/rootfs " +
                    "/bin/bash -lc " +
                    quote(
                        "export HOME=/root; " +
                        "export DISPLAY=:100; " +
                        "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                        cmd
                    )
                ).redirectErrorStream(true).start();

                result = read(p.getInputStream());
                p.waitFor();

            } catch (Exception e) {
                result = "ERROR: " + e + "\n";
            }

            final String r = result;

            runOnUiThread(() -> {
                output.append(r + "\n");
                output.append("root@metmc:~$ ");
            });

        }).start();
    }

    private static String read(InputStream in)
            throws IOException {

        ByteArrayOutputStream out =
            new ByteArrayOutputStream();

        byte[] buf = new byte[4096];
        int n;

        while ((n = in.read(buf)) != -1)
            out.write(buf,0,n);

        return out.toString("UTF-8");
    }

    private static String quote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }
}
