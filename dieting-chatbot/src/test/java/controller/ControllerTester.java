package controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

import static reactor.bus.selector.Selectors.$;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { ControllerTester.class, ControllerFactory.class })
public class ControllerTester {
    @Autowired
    private Controller controller;

    @Test
    public void testConstruct() {
        assert controller != null;
    }
    /*
    @Autowired
    private ModuleController moduleController;

    @Test
    public void testTemplateModule() {
        this.PM.setUserId("Thomas");
        this.PM.addTextMessage("abc", "hahaha");
        this.moduleController.registration();
        try {
            this.moduleController.getTemplateModule()
                .getPublisher().publishParserMessageJSON(this.PM);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } 
        assertThat(this.moduleController.getTemplateModule()
            .getMessage().getString("textContent")).isEqualTo("hahaha");
    }

    // @Test
    // public void testFormatter() {
    //     FM.set("type", "push")
    //       .set("userId", "20413459")
    //       .appendTextMessage("Hello");
    //     this.moduleController.getEventBus().on($("FormatterMessageJSON"), this.moduleController.getFormatter());
        
    //     try {
    //         this.moduleController.getPublisher()
    //             .publishFormatterMessageJSON(this.FM);
    //     } catch (InterruptedException e) {
    //         e.printStackTrace();
    //     }
    //     assertThat(this.moduleController.getFormatter()
    //         .getFormatterMessageJSON().get("UserId")).isEqualTo("agong");
        
    //     assertThat(this.moduleController.getFormatter()
    //         .getFormatterMessageJSON().getMessages()
    //         .getJsonObject(1).getString("textContent")).isEqualTo(("haha"));
    //     assertThat(this.moduleController.getFormatter()
    //         .getFormatterMessageJSON().getMessages()
    //         .getJsonObject(0).getString("originalContentUrl")).isEqualTo("abc");
        
    // }
    */
}
