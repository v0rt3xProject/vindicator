package ru.v0rt3x.vindicator.modules.web;


import ru.v0rt3x.vindicator.modules.web.handler.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WebUIPages {

    private static List<WebUIPage> webUIPages = new ArrayList<>();
    private static WebUIPage defaultPage = WebUIPage.newPage("404", null, "Page Not Found", null, new DefaultHandler());

    static {
        webUIPages.add(WebUIPage.newPage("index", "/", "Overview", "dashboard", new Index()));
        webUIPages.add(WebUIPage.newPage("flag", "/flag/", "Flag Sender", "flag", new Flag()));
        webUIPages.add(WebUIPage.newPage("exploit", "/exploit/", "Exploits", "polymer", new Exploit()));
        webUIPages.add(WebUIPage.newPage("traffic", "/traffic/", "Traffic Analyzer", "traffic", new Traffic()));
        webUIPages.add(WebUIPage.newPage("team", "/team/", "Team List", "supervisor_account", new Team()));
        webUIPages.add(WebUIPage.newPage("service", "/service/", "Services", "dns", new Service()));
        webUIPages.add(WebUIPage.newPage("agent", "/agent/", "Remote Agents", "cast", new Agent()));
        webUIPages.add(WebUIPage.newPage("terminal", "/terminal/", "Terminal", "airplay", new Terminal()));
        webUIPages.add(WebUIPage.newPage("config", "/config/", "Configuration", "build", new Config()));
    }

    public static class WebUIPage {

        private String view;
        private String link;
        private String title;
        private String icon;
        private String template;
        private String script;

        private WebUIPageHandler handler;

        public WebUIPage(String view, String link, String title, String icon, String template, String script, WebUIPageHandler handler) {
            this.view = view;
            this.link = link;
            this.title = title;
            this.icon = icon;
            this.template = template;
            this.script = script;
            this.handler = handler;
        }

        public String view() {
            return view;
        }

        public String link() {
            return link;
        }

        public String title() {
            return title;
        }

        public String icon() {
            return icon;
        }

        public String template() {
            return template;
        }

        public String script() {
            return script;
        }

        public WebUIPageHandler handler() {
            return handler;
        }

        public static WebUIPage newPage(String view, String link, String title, String icon, WebUIPageHandler handler, String template, String script) {
            return new WebUIPage(view, link, title, icon, template, script, handler);
        }

        public static WebUIPage newPage(String view, String link, String title, String icon, WebUIPageHandler handler) {
            return new WebUIPage(view, link, title, icon, String.format("%s.vm", view), String.format("%s.js", view), handler);
        }
    }

    public static WebUIPage getWebPage(String view) {
        Optional<WebUIPage> webUIPageOptional = webUIPages.stream()
            .filter(webUIPage -> webUIPage.view().equals(view)).findAny();

        if (webUIPageOptional.isPresent()) {
            return webUIPageOptional.get();
        }

        return defaultPage;
    }

    public static List<WebUIPage> getPages() {
        return webUIPages;
    }
}
