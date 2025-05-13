import AirConditionComponent from "./ACComponent";
import TemperatureSensorComponent from "@/app/components/TemperaturSensorComponent";
import MediaStationComponent from "./MediaStationComponent";
import BlindsComponent from "@/app/components/BlindsComponent";
import FridgeComponent from "./FridgeComponent";

export default function Dashboard() {
    return (
        <div className="p-6 grid grid-cols-2 gap-4">
            <AirConditionComponent/>
            <TemperatureSensorComponent/>
            <MediaStationComponent/>
            <BlindsComponent/>
            <FridgeComponent/>
        </div>
    );
}