package cz.fit.tsm.faces.common;

import org.springframework.stereotype.Component;

import cz.fit.tsm.util.Markdown;
import jakarta.inject.Named;

@Named
@Component
public class MarkdownBean {

	public String html(String markdown) {
		return Markdown.toHtml(markdown);
	}

	public String plain(String markdown) {
		return Markdown.toPlainText(markdown);
	}
}
