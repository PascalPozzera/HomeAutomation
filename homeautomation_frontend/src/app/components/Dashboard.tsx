import GridLayout from "react-grid-layout";
import "react-grid-layout/css/styles.css";
import "react-resizable/css/styles.css";

import AirConditionComponent from "./ACComponent";
import TemperatureSensorComponent from "./TemperaturSensorComponent";
import MediaStationComponent from "./MediaStationComponent";
import BlindsComponent from "./BlindsComponent";
import FridgeComponent from "./FridgeComponent";

export default function Dashboard() {
    const defaultLayout = [
        { i: "ac", x: 0, y: 0, w: 3, h: 3 },
        { i: "temp", x: 3, y: 0, w: 3, h: 3 },
        { i: "media", x: 0, y: 4, w: 3, h: 3 },
        { i: "blinds", x: 3, y: 4, w: 3, h: 3 },
        { i: "fridge", x: 9, y: 0, w: 3, h: 3 },
    ];

    const savedLayout = typeof window !== "undefined"
        ? JSON.parse(localStorage.getItem("dashboard-layout") || "null")
        : null;

    const layout = savedLayout || defaultLayout;

    const saveLayout = (layout: any) => {
        localStorage.setItem("dashboard-layout", JSON.stringify(layout));
    };

    return (
        <div
            className="mx-auto mt-2"
            style={{
                backgroundImage: "url('/background_home_automation.png')",
                backgroundRepeat: "no-repeat",
                backgroundPosition: "center",
                backgroundSize: "contain",
                width: "100vw",
                height: "45vw",
                maxHeight: "100vh",
                overflow: "hidden",
            }}
        >
            <GridLayout
                className="layout"
                layout={layout}
                cols={12}
                rowHeight={40}
                width={window.innerWidth}
                onLayoutChange={saveLayout}
                draggableHandle=".drag-handle"
                isResizable={false}
            >
                <div key="ac" className="bg-white/90 rounded-xl shadow-md p-2">
                    <div className="drag-handle cursor-move mb-1 text-center text-xs text-gray-400">☰</div>
                    <AirConditionComponent />
                </div>
                <div key="temp" className="bg-white/90 rounded-xl shadow-md p-2">
                    <div className="drag-handle cursor-move mb-1 text-center text-xs text-gray-400">☰</div>
                    <TemperatureSensorComponent />
                </div>
                <div key="media" className="bg-white/90 rounded-xl shadow-md p-2">
                    <div className="drag-handle cursor-move mb-1 text-center text-xs text-gray-400">☰</div>
                    <MediaStationComponent />
                </div>
                <div key="blinds" className="bg-white/90 rounded-xl shadow-md p-2">
                    <div className="drag-handle cursor-move mb-1 text-center text-xs text-gray-400">☰</div>
                    <BlindsComponent />
                </div>
                <div key="fridge" className="bg-white/90 rounded-xl shadow-md p-2">
                    <div className="drag-handle cursor-move mb-1 text-center text-xs text-gray-400">☰</div>
                    <FridgeComponent />
                </div>
            </GridLayout>
        </div>
    );
}
