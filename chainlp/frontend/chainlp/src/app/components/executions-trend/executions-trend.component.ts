import { Component, Input, SimpleChanges, ViewChild } from '@angular/core';
import { ChartData, ChartOptions } from 'chart.js';
import { Build } from '../../model/build.model';
import { DateTimeService } from '../../services/date-time.service';
import { BaseChartDirective } from 'ng2-charts';
import { BehaviorSubject } from 'rxjs';

@Component({
  selector: 'app-executions-trend',
  templateUrl: './executions-trend.component.html',
  styleUrl: './executions-trend.component.scss'
})
export class ExecutionsTrendComponent {

  @Input()
  builds$: BehaviorSubject<Build[]> = new BehaviorSubject<Build[]>([]);

  builds: Build[] = [];

  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  chartType: any = 'bar';
  data: ChartData<any> = {
    labels: [],
    datasets: []
  };
  trendChartOptions: ChartOptions<any> = {
    responsive: true,
    maintainAspectRatio: false,
    barPercentage: 0.05,
    scales: {
      x: {
        stacked: true,
        grid: {
          display: false,
        }
      },
      y: {
        stacked: true,
        grid: {
          display: false,
        }
      }
    },
    plugins: {
      legend: {
        display: false
      }
    }
  };

  constructor(private datetimeService: DateTimeService) { }

  ngOnInit() {
    this.builds$.subscribe((builds) => {
      this.builds = builds;
      this.showTrends();
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    
    //this.showTrends();
    //this.chart?.update();
  }

  showTrends() {
    this.data = {
      labels: [],
      datasets: []
    };
    const data: any = {};
    this.builds.forEach((build) => {
      const dt = this.datetimeService.formatDate(build.startedAt, 'MMM/DD');
      const project = build.projectName;
      if (dt in data) {
        if (data[dt].some((d: any) => d.project === project)) {
          data[dt].find((d: any) => d.project === project).executions++;
        } else {
          data[dt].push({
            project: project,
            executions: 1
          });
        }
      } else {
        data[dt] = [{
          project: project,
          executions: 1
        }]
      }
    });
    for (let o in data) {
      this.data.labels?.push(o);
      data[o].forEach((d: any) => {
        if (this.data.datasets.some((ds: any) => ds.label === d.project)) {
          this.data.datasets.find((ds: any) => ds.label === d.project).data.push(d.executions);
        } else {
          this.data.datasets.push({
            label: d.project,
            data: [d.executions]
          });
        }
      });
    }
  }
  
}
