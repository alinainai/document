大意的意思是处理 4 个表格中的和 url 对应的数据，并整理到同一个表格。

```python
import xlrd
import xlwt

def readContentFromXls():
    # 请求次数的表格
    request_count = xlrd.open_workbook('./request_count.xls').sheet_by_index(0)
    # api tp 时间的表格
    api_tp= xlrd.open_workbook('./api_tp.xls').sheet_by_index(0)
    # 请求返回长度表格
    api_conten_length= xlrd.open_workbook('./api_conten_length.xls').sheet_by_index(0)
    # 请求返回长度表格
    result_table= xlrd.open_workbook('./result.xls').sheet_by_index(0)
    
    workbook = xlwt.Workbook(encoding= 'UTF-8')
    worksheet = workbook.add_sheet("result")
    
    # 各个表格行数，遍历用
    request_count_nrows = request_count.nrows
    api_tp_nrows = api_tp.nrows
    api_conten_length_nrows = api_conten_length.nrows
    result_table_nrows = result_table.nrows
    
    # 存储url 和 各个值的映射关系
    request_count_dic = {}
    api_tp_dic = {}
    api_conten_max_dic = {}
    api_conten_ave_dic = {}
    
    for i in range(1, request_count_nrows):
        str1 = str(request_count.cell_value(i, 1)).lower().strip()
        if(str1.isspace() == True):
            continue
        request_count_dic[str1]=str(request_count.cell_value(i, 2)).strip()
#        print(str1,request_count_dic[str1])
        
    for i in range(1, api_tp_nrows):
        str1 = str(api_tp.cell_value(i, 1)).lower().strip()
        if(str1.isspace() == True):
            continue
        api_tp_dic[str1]=str(api_tp.cell_value(i, 2)).strip()
#        print(str1,api_tp_dic[str1])

    for i in range(1, api_conten_length_nrows):
        str1 = str(api_conten_length.cell_value(i, 1)).lower().strip()
        if(str1.isspace() == True):
            continue
        api_conten_max_dic[str1]=str(api_conten_length.cell_value(i, 3)).strip()
#        print(str1,api_conten_max_dic[str1])
        
    for i in range(1, api_conten_length_nrows):
        str1 = str(api_conten_length.cell_value(i, 1)).lower().strip()
        if(str1.isspace() == True):
            continue
        api_conten_ave_dic[str1]=str(api_conten_length.cell_value(i, 2)).strip()
#        print(str1,api_conten_ave_dic[str1])

    for i in range(1, result_table_nrows):
        str1 = str(result_table.cell_value(i, 1)).lower().strip()
        if(str1.isspace() == True):
            continue
        worksheet.write(i,0, str(result_table.cell_value(i, 1)))
        if(str1 in api_tp_dic and api_tp_dic[str1]):
            worksheet.write(i,1, str(api_tp_dic[str1]))
        if(str1 in request_count_dic and request_count_dic[str1]):
            worksheet.write(i,2, str(request_count_dic[str1]))
        if(str1 in api_conten_ave_dic and api_conten_ave_dic[str1]):
            worksheet.write(i,3, str(api_conten_ave_dic[str1]))
        if(str1 in api_conten_max_dic and api_conten_max_dic[str1]):
            worksheet.write(i,4, str(api_conten_max_dic[str1]))

    workbook.save("./learn.xls")
        


def main():
    # 从 xls 中读取值并存储到 map 中
    readContentFromXls()
    
if __name__== "__main__" :
    main()
```
