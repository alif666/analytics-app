export default async function Page(){

    // DAY 1 exercises
    type meterReading =  {
        meterId: string;
        timestamp: string;
        consumptionKwh : number; 
    }

    function calculateTotal(meterReadings:meterReading[]):number{
        return meterReadings.reduce((total, reading)=>total+reading.consumptionKwh,0);
    }

    function calculateAverage(meterReadings: meterReading[]): number{
        if(meterReadings.length===0)return 0;
        return calculateTotal(meterReadings)/meterReadings.length;
    }

    function findPeakReading(meterReadings: meterReading[]):meterReading{
        return meterReadings.reduce((peak,current)=> peak.consumptionKwh >current.consumptionKwh?peak:current);
    }

    function calculateMinimumUsage(readings:meterReading[]):number | undefined{
        if (readings.length===0)  return undefined;
        return readings.reduce((minimum,current)=>minimum<current.consumptionKwh? minimum:current.consumptionKwh,readings[0].consumptionKwh);
    }

    function filterHigherUsage(readings:meterReading[],threshold:number):meterReading[]{
        return readings.filter(reading=> reading.consumptionKwh>threshold)
    }

    function getConsumptionValues(readings:meterReading[]):number[]{
        return readings.map(reading=> reading.consumptionKwh);
    }


    return(
        <h1 className="text-foreground">Meter Page</h1>
    );
}
