package at.fhv.sysarch.lab2.homeautomation.controller;

import akka.actor.typed.ActorRef;
import at.fhv.sysarch.lab2.homeautomation.devices.ac.AirCondition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HomeAutomationController {

    private final ActorRef<AirCondition.AirConditionCommand> airCondition;

    public HomeAutomationController(ActorRef<AirCondition.AirConditionCommand> airCondition) {
        this.airCondition = airCondition;
    }

    //use ask instead of tell if a response is expected
    @PostMapping("/mode")
    public ResponseEntity<Void> switchMode(@RequestParam boolean simulate) {
        airCondition.tell(new AirCondition.SwitchSensorMode(simulate));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/temperature")
    public ResponseEntity<Void> sendTemperature(@RequestParam double value) {
        airCondition.tell(new AirCondition.EnrichedTemperature(value, "Celsius"));
        return ResponseEntity.ok().build();
    }
}
