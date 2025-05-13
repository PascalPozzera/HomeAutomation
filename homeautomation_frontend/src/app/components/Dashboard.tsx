"use client";

import AirConditionComponent from "./ACComponent";
import TemperatureSensorComponent from "./TemperaturSensorComponent";
import MediaStationComponent from "./MediaStationComponent";
import BlindsComponent from "./BlindsComponent";
import FridgeComponent from "./FridgeComponent";

export default function Dashboard() {
    return (
        <div className="flex items-center justify-center min-h-screen p-6 bg-gray-50 gap-10">
            {/* Linke Spalte */}
            <div className="flex flex-col gap-6 items-center">
                <div className="w-64 h-64">
                    <AirConditionComponent />
                </div>
                <div className="w-64 h-64">
                    <MediaStationComponent />
                </div>
                <div className="w-64 h-64">
                    <BlindsComponent />
                </div>
            </div>

            {/* Hintergrundbild */}
            <div
                className="w-[45vw] h-[45vw] bg-no-repeat bg-center bg-contain"
                style={{
                    backgroundImage: "url('/background_home_automation.png')",
                }}
            />

            {/* Rechte Spalte */}
            <div className="flex flex-col gap-6 items-center">
                <div className="w-64 h-64">
                    <FridgeComponent />
                </div>
                <div className="w-64 h-64">
                    <TemperatureSensorComponent />
                </div>
            </div>
        </div>
    );
}
