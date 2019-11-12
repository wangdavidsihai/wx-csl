package com.csl.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.csl.constants.MenuKey;
import com.csl.handler.ChangeNewsHandler;
import com.csl.handler.ConfigHander;
import com.csl.handler.HelpDocHandler;
import com.csl.handler.RankHandler;
import com.csl.handler.WhoAmIHandler;
import com.csl.matcher.WhoAmIMatcher;
import com.soecode.wxtools.api.IService;
import com.soecode.wxtools.api.WxConsts;
import com.soecode.wxtools.api.WxMessageRouter;
import com.soecode.wxtools.api.WxService;
import com.soecode.wxtools.bean.WxXmlMessage;
import com.soecode.wxtools.bean.WxXmlOutMessage;
import com.soecode.wxtools.util.xml.XStreamTransformer;

@RestController
@RequestMapping("/wx")
public class WxController {

	private IService iService = new WxService();

	@GetMapping
	public String check(String signature, String timestamp, String nonce, String echostr) {
		System.out.println("Check authentication code. ");
		if (iService.checkSignature(signature, timestamp, nonce, echostr)) {
			return echostr;
		}
		return "Access denied.";
	}

	@PostMapping
	public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();

		// 创建一个路由器
		WxMessageRouter router = new WxMessageRouter(iService);
		try {
			// 微信服务器推送过来的是XML格式。
			WxXmlMessage wx = XStreamTransformer.fromXml(WxXmlMessage.class, request.getInputStream());
			System.out.println("Received message：\n " + wx.toString());
			router.rule().msgType(WxConsts.XML_MSG_TEXT).matcher(new WhoAmIMatcher()).handler(new WhoAmIHandler()).end()
					.rule().msgType(WxConsts.XML_MSG_TEXT).handler(ConfigHander.getInstance()).end().rule()
					.event(WxConsts.EVT_CLICK).eventKey(MenuKey.HELP).handler(HelpDocHandler.getInstance()).next()
					.rule().eventKey(MenuKey.CHANGE_NEWS).handler(ChangeNewsHandler.getInstance()).next().rule()
					.eventKey(MenuKey.HOT_SONG).handler(RankHandler.getInstance()).next().rule()
					.eventKey(MenuKey.TOP_500).handler(RankHandler.getInstance()).next().rule()
					.eventKey(MenuKey.NET_HOT_SONG).handler(RankHandler.getInstance()).next().rule()
					.eventKey(MenuKey.HUAYU_SONG).handler(RankHandler.getInstance()).next().rule()
					.eventKey(MenuKey.XINAO_SONG).handler(RankHandler.getInstance()).end();
			// 把消息传递给路由器进行处理
			WxXmlOutMessage xmlOutMsg = router.route(wx);
			if (xmlOutMsg != null)
				out.print(xmlOutMsg.toXml());// 因为是明文，所以不用加密，直接返回给用户。

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			out.close();
		}

	}

}
