import type { Meta, StoryObj } from '@storybook/vue3'
import { ElButton, ElInput, ElSelect, ElDatePicker, ElTable, ElTree, ElDialog, ElDrawer, ElTabs, ElTabPane, ElSteps, ElStep, ElUpload, ElPagination, ElTag, ElTooltip, ElNotification, ElMessage, ElSkeleton, ElEmpty, ElCarousel, ElCarouselItem, ElImage, ElCascader, ElTransfer, ElTimeline, ElTimelineItem, ElDescriptions, ElDescriptionsItem, ElStatistic, ElProgress, ElAvatar, ElDivider } from 'element-plus'

const meta: Meta = { title: 'Element Plus/Wrappers' }
export default meta

export const ButtonDefault: StoryObj = { render: () => ({ components: { ElButton }, template: '<ElButton>默认</ElButton>' }) }
export const ButtonPrimary: StoryObj = { render: () => ({ components: { ElButton }, template: '<ElButton type="primary">主要</ElButton>' }) }
export const ButtonDisabled: StoryObj = { render: () => ({ components: { ElButton }, template: '<ElButton disabled>禁用</ElButton>' }) }
export const InputDefault: StoryObj = { render: () => ({ components: { ElInput }, template: '<ElInput placeholder="输入" />' }) }
export const InputClearable: StoryObj = { render: () => ({ components: { ElInput }, template: '<ElInput clearable placeholder="可清除" />' }) }
export const SelectSingle: StoryObj = { render: () => ({ components: { ElSelect }, template: '<ElSelect placeholder="选择" />' }) }
export const DatePicker: StoryObj = { render: () => ({ components: { ElDatePicker }, template: '<ElDatePicker type="date" />' }) }
export const TableBasic: StoryObj = {
  render: () => ({
    components: { ElTable },
    template: '<ElTable :data="[{name:\'A\'},{name:\'B\'}]"><el-table-column prop="name" label="名称" /></ElTable>',
  }),
}
export const TreeDefault: StoryObj = {
  render: () => ({
    components: { ElTree },
    data: () => ({ data: [{ label: '根', children: [{ label: '子' }] }] }),
    template: '<ElTree :data="data" />',
  }),
}
export const DialogDefault: StoryObj = { render: () => ({ components: { ElDialog }, template: '<ElDialog model-value title="对话框" />' }) }
export const DrawerRight: StoryObj = { render: () => ({ components: { ElDrawer }, template: '<ElDrawer model-value title="抽屉" />' }) }
export const TabsDefault: StoryObj = {
  render: () => ({
    components: { ElTabs, ElTabPane },
    template: '<ElTabs><ElTabPane label="Tab1">内容1</ElTabPane><ElTabPane label="Tab2">内容2</ElTabPane></ElTabs>',
  }),
}
export const StepsHorizontal: StoryObj = {
  render: () => ({
    components: { ElSteps, ElStep },
    template: '<ElSteps :active="1"><ElStep title="步骤1" /><ElStep title="步骤2" /></ElSteps>',
  }),
}
export const UploadFile: StoryObj = { render: () => ({ components: { ElUpload }, template: '<ElUpload action="#">上传</ElUpload>' }) }
export const PaginationDefault: StoryObj = { render: () => ({ components: { ElPagination }, template: '<ElPagination :total="100" />' }) }
export const TagDefault: StoryObj = { render: () => ({ components: { ElTag }, template: '<ElTag>标签</ElTag>' }) }
export const TooltipTop: StoryObj = {
  render: () => ({
    components: { ElTooltip, ElButton },
    template: '<ElTooltip content="提示"><ElButton>悬停</ElButton></ElTooltip>',
  }),
}
export const NotificationSuccess: StoryObj = {
  render: () => ({
    mounted() { ElNotification.success({ title: '成功', message: '操作完成' }) },
    template: '<div />',
  }),
}
export const MessageWarning: StoryObj = {
  render: () => ({
    mounted() { ElMessage.warning('警告消息') },
    template: '<div />',
  }),
}
export const SkeletonDefault: StoryObj = { render: () => ({ components: { ElSkeleton }, template: '<ElSkeleton :rows="3" animated />' }) }
export const EmptyDefault: StoryObj = { render: () => ({ components: { ElEmpty }, template: '<ElEmpty description="暂无数据" />' }) }
export const CarouselDefault: StoryObj = {
  render: () => ({
    components: { ElCarousel, ElCarouselItem },
    template: '<ElCarousel height="120px"><ElCarouselItem v-for="i in 3" :key="i"><h3>{{ i }}</h3></ElCarouselItem></ElCarousel>',
  }),
}
export const ImagePreview: StoryObj = { render: () => ({ components: { ElImage }, template: '<ElImage style="width:100px;height:100px" src="https://via.placeholder.com/100" />' }) }
export const CascaderDefault: StoryObj = { render: () => ({ components: { ElCascader }, template: '<ElCascader placeholder="级联" />' }) }
export const TransferDefault: StoryObj = { render: () => ({ components: { ElTransfer }, template: '<ElTransfer />' }) }
export const TimelineDefault: StoryObj = {
  render: () => ({
    components: { ElTimeline, ElTimelineItem },
    template: '<ElTimeline><ElTimelineItem>事件1</ElTimelineItem></ElTimeline>',
  }),
}
export const DescriptionsDefault: StoryObj = {
  render: () => ({
    components: { ElDescriptions, ElDescriptionsItem },
    template: '<ElDescriptions title="详情"><ElDescriptionsItem label="名称">值</ElDescriptionsItem></ElDescriptions>',
  }),
}
export const StatisticNumber: StoryObj = { render: () => ({ components: { ElStatistic }, template: '<ElStatistic title="KPI" :value="1234" />' }) }
export const ProgressLine: StoryObj = { render: () => ({ components: { ElProgress }, template: '<ElProgress :percentage="70" />' }) }
export const AvatarDefault: StoryObj = { render: () => ({ components: { ElAvatar }, template: '<ElAvatar>U</ElAvatar>' }) }
export const DividerHorizontal: StoryObj = { render: () => ({ components: { ElDivider }, template: '<span>上</span><ElDivider /><span>下</span>' }) }
