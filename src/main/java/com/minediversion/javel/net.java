package com.minediversion.javel;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import okhttp3.*;
import okhttp3.internal.JavaNetCookieJar;
import okhttp3.logging.HttpLoggingInterceptor;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class net {
    public static Path cookiePath = Path.of(System.getenv("APPDATA") + "\\JetBrains\\JutgePlugin\\tkn");

    public static boolean isLoginAvailable(){return Files.exists(credEncryption.cred);}

    @SuppressWarnings("KotlinInternalInJava")
    public static void logIn(credEncryption.Credentials credentials, ToolWindow toolWindow) {
        try {
            HttpUrl url = HttpUrl.parse("https://jutge.org/");

            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieJar cookieJar = new JavaNetCookieJar(cookieManager);
            cookieJar.loadForRequest(url);

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(@NotNull String message) {
                    //System.out.println(message);
                }
            });
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient().newBuilder()
                    .cookieJar(cookieJar)
                    .addInterceptor(interceptor)
                    .build();
            RequestBody body = new FormBody.Builder()
                    .add("email", credentials.usr.getText())
                    .add("password", String.valueOf(credentials.pass.getPassword()))
                    .add("submit", "")
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();
            if(response.body() != null){
                if(response.body().string().contains("Jutge.org - Dashboard")){
                    JutgeNotificationHandler.notifyInfo(toolWindow.getProject(), "LogIn Successful");
                    if(!isLoginAvailable()) credEncryption.createCredFile(credentials, toolWindow);
                    Files.createFile(cookiePath);
                    Files.write(cookiePath, String.valueOf(cookieJar.loadForRequest(url).get(0)).getBytes());
                }else JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Wrong Credentials");
            }else JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Http Error");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("KotlinInternalInJava")
    public static String checkToken(String cookie, ToolWindow toolWindow) {
        try {
            HttpUrl url = HttpUrl.parse("https://jutge.org/dashboard");

            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieJar cookieJar = new JavaNetCookieJar(cookieManager);
            List<Cookie> list = new ArrayList<>();
            list.add(Cookie.parse(url, cookie));
            cookieJar.saveFromResponse(url, list);
            cookieJar.loadForRequest(url);

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(@NotNull String message) {
                    //System.out.println(message);
                }
            });
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient().newBuilder()
                    .cookieJar(cookieJar)
                    .addInterceptor(interceptor)
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            if(response.body() != null){
                String body = response.body().string();
                if(!body.contains("Did you sign in")) return body;
                else{
                    JutgeNotificationHandler.notifyInfo(toolWindow.getProject(), "Token Expired/Invalid");
                    return null;
                }
            }else JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Http Error");
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("KotlinInternalInJava")
    public static List<String> getProblem(String cookie, String problemId,ToolWindow toolWindow) {
        try {
            HttpUrl url = HttpUrl.parse("https://jutge.org/problems/"+problemId);

            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieJar cookieJar = new JavaNetCookieJar(cookieManager);
            List<Cookie> list = new ArrayList<>();
            list.add(Cookie.parse(url, cookie));
            cookieJar.saveFromResponse(url, list);
            cookieJar.loadForRequest(url);

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(@NotNull String message) {
                    //System.out.println(message);
                }
            });
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient().newBuilder()
                    .cookieJar(cookieJar)
                    .addInterceptor(interceptor)
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            if(response.body() != null){
                List<String> problemStats = new ArrayList<>();
                String body = response.body().string();
                if(body.contains("Wrong URL")){
                    JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Wrong Problem Id");
                    return null;
                }else if (body.contains("Quiz")){
                    JutgeNotificationHandler.notifyInfo(toolWindow.getProject(), "Quiz are not supported");
                    return null;
                }
                if(problemId.charAt(0) == 'P') {
                    problemStats.add(body.split("</title>")[0].split(" - ")[2]); //Title
                    problemStats.add(body.split("<div class='panel-heading'>\n" +
                            "                Problem ")[1]
                            .split("""
                                    </div>
                                                <div class='panel-body'>
                                                    <table>""")[0]
                            .split(": ")[1]);//Status
                    problemStats.add(body.split("<!--HEVEA command line is: hevea html.tex -->\n" +
                            "<!--CUT STYLE article--><!--CUT DEF section 1 --><p>")[1].split("</p><!--CUT END -->\n" +
                            "<!--HTMLFOOT-->\n" +
                            "<!--ENDHTML-->\n" +
                            "<!--FOOTER-->")[0]);//Summary/Expected I/O
                    problemStats.add(body.split("<div class='list-group'>")[1]
                            .split("</li>\n" +
                                    "            \n" +
                                    "                </div>\n" +
                                    "            </div>")[0]);//Public Test Case
                }else{
                    problemStats.add(body.split("</title>")[0].split(" - ")[2]); //Title
                    problemStats.add(body.split("<div class='panel-heading'>\n" +
                            "                Problem ")[1]
                            .split("""
                                    </div>
                                                <div class='panel-body'>
                                                    <table>""")[0]
                            .split(": ")[1]);//Status
                    problemStats.add(body.split("<!--CUT STYLE article--><!--CUT DEF section 1 -->")[1]
                            .split("""
                                    <!--CUT END -->
                                    <!--HTMLFOOT-->
                                    <!--ENDHTML-->
                                    <!--FOOTER-->""")[0]);//Statement
                    problemStats.add(body.split("<div class='panel-body' style='padding: 0px;'>")[1]
                            .split("""
                                    </pre></div>
                                    </pre>
                                                    </div>
                                                </div>""")[0]);//Sample Session
                }
                problemStats.add(body.split("<div class='col-sm-4'>\n" +
                        "                            <input name='token_uid' type='hidden' value='")[1]
                        .split("' />\n" +
                                "                            <button id='submit' name='submit' type='btn' class='btn btn-block btn-primary btn-sm btn-center'>")[0]);
                //token_uid
                return problemStats;
            }else JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Http Error");
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("KotlinInternalInJava")
    public static void sendFile(String cookie, String problemId, VirtualFile virtualFile, String uploadToken, ToolWindow toolWindow) {
        try {
            HttpUrl url = HttpUrl.parse("https://jutge.org/problems/"+problemId+"/submissions");

            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieJar cookieJar = new JavaNetCookieJar(cookieManager);
            List<Cookie> list = new ArrayList<>();
            list.add(Cookie.parse(url, cookie));
            cookieJar.saveFromResponse(url, list);
            cookieJar.loadForRequest(url);

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(@NotNull String message) {
                    //System.out.println(message);
                }
            });
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient().newBuilder()
                    .cookieJar(cookieJar)
                    .addInterceptor(interceptor)
                    .build();
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", virtualFile.getName(),
                            RequestBody.create(MediaType.parse("application/octet-stream"), new File(virtualFile.getPath())))
                    .addFormDataPart("annotation", "")
                    .addFormDataPart("compiler_id", "Python3")
                    .addFormDataPart("token_uid", uploadToken)
                    .addFormDataPart("submit", "")
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();
            if(response.body() != null){
                if (!response.body().string().contains("Judging submission"))
                    JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Upload Error");
                else{
                    JutgeNotificationHandler.notifyInfo(toolWindow.getProject(), "Upload Successful");
                }
            }else JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Http Error");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
