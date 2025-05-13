import AirConditionComponent from "./AC";
import TemperatureSensorComponent from "@/app/components/TemperaturSensor";

export default function Dashboard() {
    return (
        <div className="p-6 grid grid-cols-2 gap-4">
            <AirConditionComponent/>
            <TemperatureSensorComponent/>
        </div>
    );
}