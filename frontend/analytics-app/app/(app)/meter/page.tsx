import {ready} from "next/dist/build/output/log";

export default async function Page(){

    // DAY 1 exercises
    type MeterReading =  {
        meterId: string;
        timestamp: string;
        consumptionKwh : number; 
    }

    function calculateTotal(meterReadings:MeterReading[]):number{
        return meterReadings.reduce((total, reading)=>total+reading.consumptionKwh,0);
    }

    function calculateAverage(meterReadings: MeterReading[]): number{
        if(meterReadings.length===0)return 0;
        return calculateTotal(meterReadings)/meterReadings.length;
    }

    function findPeakReading(meterReadings: MeterReading[]):MeterReading{
        return meterReadings.reduce((peak,current)=> peak.consumptionKwh >current.consumptionKwh?peak:current);
    }

    function calculateMinimumUsage(readings:MeterReading[]):number | undefined{
        if (readings.length===0)  return undefined;
        return readings.reduce((minimum,current)=>minimum<current.consumptionKwh? minimum:current.consumptionKwh,readings[0].consumptionKwh);
    }

    function filterHigherUsage(readings:MeterReading[],threshold:number):MeterReading[]{
        return readings.filter(reading=> reading.consumptionKwh>threshold)
    }

    function getConsumptionValues(readings:MeterReading[]):number[]{
        return readings.map(reading=> reading.consumptionKwh);
    }

    type LoadingState = "IDLE" | "LOADING";

    type ApiState =
        |{
            status: "Idle"
        }
        |{
            status: "Loading"
        }
        |{
            status: "Success",
            data: MeterReading[]
        }
        |{
            status: "Failed",
            error: string
        };

    type weakApiState = {
        status: string;
        data: MeterReading[];
        error?: string;
    }

    function renderState(state:ApiState){
        switch(state.status){
            case "Idle":
                return "Ready";
            case "Loading":
                return "Loading";
            case "Success":
                return `${state.data.length} readings`;
            case "Failed":
                return `${state.error} Failed`;
            default:  assertNever(state);
        }
    }

    // User Authentication State
    type ChartPoint = {
        timestamp: string;
        consumptionKwh: number;
    }

    type ChartState  =
        |{
        status : "idle";
    }|{
        status: "loading";

    }|{
        status: "success";
        data: ChartPoint[];
    }|{
        status: "failed";
        error: string;
    }

    function renderChartState(state: ChartState){
        switch(state.status){
            case "idle":
                return  "Ready to draw";
            case "loading":
                return "Drawing chart  points";
            case "success":
                return  `chart drew with ${state.data.length} points`;
            case "failed":
                return `failed to draw chart for ${state.error} reason`;
            default:  assertNever(state);
        }
    }


    function assertNever(value:  never):never{
        throw new Error(`${JSON.stringify(value)} is never`);
    }

    function formatMeterIdToNumber(meterId:number|string):number{
        return Number(meterId);
    }

    function formatMeterIdToString(meterId: number|string):string{
        return (meterId+"");
    }

    function normalizeValue(value: number|string):number{
        if(typeof value==="number"){
            return value;
        }
    return Number(value);
    }

    type Success = {
        data: MeterReading[];
    }

    type Failure = {
        error: Error;
    }

    function processFailure(result: Success| Failure):void{
        if("data" in result){
            console.log("Success Result");
        }else if("error" in  result){
            console.log("Failure Result");
        }
    }

    function formatError(error: Error| string){
        if(error instanceof Error){
            return error.message;
        }
        return error;
    }

    // console.log('null and unddefined');
    // console.log(typeof  null);
    // console.log(typeof  undefined);
    // console.log(null==undefined);
    // console.log(null===undefined)
    //

    function createGame(){
        let  score = 0;
        function increaseScore(point:number){
            console.log("increased point +",point);
            return score += point;
        }

        function decreaseScore(point:number){
            console.log("decreased point -",point);
            return score -= point;
        }

        function getScore(){
            console.log("Final score is ",score);
            return score;
        }
        return {increaseScore, decreaseScore, getScore}
    }

    const game1 = createGame();
    const game2 = createGame();

    game1.increaseScore(3);
    game2.increaseScore(2);
    game1.decreaseScore(1);
    game2.decreaseScore(3);
    game1.getScore();
    game2.getScore();





    return(
        <h1 className="text-foreground">Meter Page</h1>
    );
}
