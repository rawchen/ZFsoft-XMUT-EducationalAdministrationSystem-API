import java.util.List;

/**
 * 程序入口
 * 警示：使用前请修改账号密码
 * 		如果你不是厦门理工学生请在项目中批量替换“jw.xmut.edu.cn”为你所在学校
 */
public class Main {
	public static void main(String[] args) {
		ZFsoft zFsoft=new ZFsoft();

		zFsoft.login("输入你的教务系统学号","输入你的教务系统密码");

		//测试查询成绩
		List<Score> scoreList = zFsoft.checkScore("2018","");
		for(Score score:scoreList){
			System.out.println(score);
		}
		System.out.println("共有条目：" + scoreList.size());

		//测试获取学生信息
		zFsoft.getStudentInformation();

		//测试查询课表
		zFsoft.checkTimetable("2018","1");

	}
}