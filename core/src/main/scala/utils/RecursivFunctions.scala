package clustering4ever.util

object RecursivFunctions
{
  @annotation.tailrec
  def goOverMatrix[T](i: Int, j: Int, t: T, sizeColumn: Int, f: (Int, Int, T) => T): T =
  {
    if( i >= 0 && j > 0 ) goOverMatrix(i, j - 1, f(i, j, t), sizeColumn, f)
    else if( i > 0 && j == 0 ) goOverMatrix(i - 1, sizeColumn - 1, f(i, j, t), sizeColumn, f)
    else f(i, j, t)       
  }	
}